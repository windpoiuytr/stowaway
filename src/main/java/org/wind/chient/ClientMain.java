package org.wind.chient;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.wind.chient.core.ClientNetty;
import org.wind.util.Config;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientMain
{
	// Netty 服务
	private static ClientNetty clientNetty;
	// 当前状态是否启用（true = 已启用，false = 已禁用），原子变量，防止线程冲突
	private static final AtomicBoolean isEnabled = new AtomicBoolean(false);
	// 标志是否正在执行某个操作
	private static final AtomicBoolean isProcessing = new AtomicBoolean(false);
	// 托盘图标（用于后续访问和更新）
	private static TrayIcon trayIcon;
	// 启用状态图标
	private static Image iconEnabled;
	// 禁用状态图标
	private static Image iconDisabled;
	// 加载状态图标
	private static Image iconLoading;

	public static void main(String[] args)
	{
		// Swing UI 库
		FlatDarculaLaf.setup();

		try
		{
			// 获取配置文件
			Config.get();
		} catch (Exception e)
		{
			// 弹出桌面提示框
			javax.swing.JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					"获取配置文件失败",
					javax.swing.JOptionPane.ERROR_MESSAGE
			);
			throw new RuntimeException("获取配置文件失败", e);
		}

		// Netty 服务
		clientNetty = new ClientNetty(8888);

		// 获取当前工作目录
		String baseDir = System.getProperty("user.dir");

		// 加载图标资源
		iconEnabled = Toolkit.getDefaultToolkit().getImage(baseDir + "/icon_enabled.png");
		iconDisabled = Toolkit.getDefaultToolkit().getImage(baseDir + "/icon_disabled.png");
		iconLoading = Toolkit.getDefaultToolkit().getImage(baseDir + "/icon_loading.gif");

		// 创建托盘图标（初始为禁用状态）
		trayIcon = new TrayIcon(iconDisabled);
		trayIcon.setImageAutoSize(true);

		// 添加点击事件
		trayIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// 不阻塞 EDT，始终能点进来
				if (isProcessing.get())
				{
					return; // 操作进行中，忽略本次点击
				}

				isProcessing.set(true);

				// 显示加载图标
				trayIcon.setImage(iconLoading);

				if (e.getButton() == MouseEvent.BUTTON1)
				{
					new Thread(() ->
					{
						try
						{
							if (isEnabled.get())
							{
								disable();
							} else
							{
								enable();
							}
						} finally
						{
							isProcessing.set(false);
						}
					}).start();
				}
				// 右键退出
				else if (e.getButton() == MouseEvent.BUTTON3)
				{
					new Thread(() ->
					{
						try
						{
							disable();
							// 移除托盘
							SystemTray.getSystemTray().remove(trayIcon);
						} finally
						{
							isProcessing.set(false);
						}
					}).start();
				}
			}
		});

		// 添加到系统托盘
		try
		{
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e)
		{
			// 弹出桌面提示框
			javax.swing.JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					"无法添加系统托盘图标",
					javax.swing.JOptionPane.ERROR_MESSAGE
			);
			throw new RuntimeException("无法添加系统托盘图标", e);
		}

		// 添加关闭钩子（确保系统关机/重启时清理）
		Runtime.getRuntime().addShutdownHook(new Thread(clientNetty::disable));

		// 默认开启
		enable();
	}

	/**
	 * 启用服务和系统代理
	 */
	public static void enable()
	{
		if (!isEnabled.get())
		{
			clientNetty.enable();
			trayIcon.setImage(iconEnabled);
			isEnabled.set(true);
		}
	}

	/**
	 * 禁用服务和系统代理
	 */
	public static void disable()
	{
		if (isEnabled.get())
		{
			clientNetty.disable();
			trayIcon.setImage(iconDisabled);
			isEnabled.set(false);
		}
	}
}