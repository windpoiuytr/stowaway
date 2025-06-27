package org.wind.chient;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.wind.chient.core.ClientNetty;
import org.wind.util.Config;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClientMain
{
	// Netty 服务
	private static ClientNetty clientNetty;
	// 当前状态是否启用（true = 已启用，false = 已禁用）
	private static boolean isEnabled = false;
	// 托盘图标（用于后续访问和更新）
	private static TrayIcon trayIcon;
	// 启用状态图标
	private static Image iconEnabled;
	// 禁用状态图标
	private static Image iconDisabled;

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

		// 创建托盘图标（初始为禁用状态）
		trayIcon = new TrayIcon(iconDisabled);
		trayIcon.setImageAutoSize(true);

		// 添加点击事件
		trayIcon.addMouseListener(new MouseAdapter()
		{
			private long lastClickTime = 0;

			@Override
			public void mouseClicked(MouseEvent e)
			{
				long now = System.currentTimeMillis();
				if (now - lastClickTime < 1000) return; // 防抖：1秒内不响应重复点击
				lastClickTime = now;

				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (isEnabled)
						disable();
					else
						enable();
				}
				// 右键退出
				else if (e.getButton() == MouseEvent.BUTTON3)
				{
					clientNetty.disable();
					// 移除托盘
					SystemTray.getSystemTray().remove(trayIcon);
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
		if (!isEnabled)
		{
			isEnabled = true;
			trayIcon.setImage(iconEnabled);
			clientNetty.enable();
		}
	}

	/**
	 * 禁用服务和系统代理
	 */
	public static void disable()
	{
		if (isEnabled)
		{
			isEnabled = false;
			trayIcon.setImage(iconDisabled);
			clientNetty.disable();
		}
	}
}