package org.wind.chient.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.wind.chient.util.SystemProxy;

import javax.swing.*;
import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientNetty
{

	private final int port;  // 服务监听端口
	private final AtomicBoolean started = new AtomicBoolean(false); // 防止重复启动
	private EventLoopGroup bossGroup;    // 主线程组（处理连接）
	private EventLoopGroup workerGroup;  // 工作线程组（处理数据）
	private Channel serverChannel;       // 服务器主 Channel 引用

	public ClientNetty(int port)
	{
		this.port = port;
	}

	/**
	 * 启动 Netty 服务端
	 */
	public void enable()
	{
		// 避免重复启动
		if (started.get())
		{
			// System.out.println("Netty 服务已启动，无需重复启动");
			return;
		}

		// 创建线程组
		bossGroup = new NioEventLoopGroup(1); // 单线程接收连接
		workerGroup = new NioEventLoopGroup(); // 默认线程数 = CPU * 2

		try
		{
			// 配置启动器
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap
					.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_REUSEADDR, true) // 允许端口复用
					.childOption(ChannelOption.SO_KEEPALIVE, true) // 长连接心跳
					.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.childHandler(new ChannelInitializer<SocketChannel>()
					{
						@Override
						protected void initChannel(SocketChannel socketChannel)
						{
							// 添加最初的处理器
							socketChannel.pipeline().addLast(new ClientNettyRead1());
						}
					});

			// 绑定端口并同步等待绑定成功
			ChannelFuture future = bootstrap.bind(port).sync();
			serverChannel = future.channel(); // 保存引用，便于关闭
			started.set(true);

			System.out.println("[INFO] Netty 启动成功，监听端口：" + port);

			// 启用系统代理
			SystemProxy.enable(port);

		} catch (BindException e)
		{
			showError("端口已被占用：" + port, e);
		} catch (IOException e)
		{
			showError("启用系统代理失败", e);
		} catch (InterruptedException e)
		{
			showError("Netty 启动中断", e);
			Thread.currentThread().interrupt(); // 保留中断状态
		} catch (Exception e)
		{
			showError("未知异常", e);
		}
	}

	/**
	 * 停止 Netty 服务端
	 */
	public void disable()
	{
		if (!started.get())
		{
			// System.out.println("Netty 服务未启动，无需关闭");
			return;
		}

		// System.out.println("[INFO] 正在关闭 Netty 服务...");

		try
		{
			// 禁用系统代理
			SystemProxy.disable();

			// 优雅关闭 Netty
			if (serverChannel != null && serverChannel.isOpen())
			{
				serverChannel.close().syncUninterruptibly();
			}
			if (bossGroup != null)
			{
				bossGroup.shutdownGracefully().syncUninterruptibly();
			}
			if (workerGroup != null)
			{
				workerGroup.shutdownGracefully().syncUninterruptibly();
			}
		} catch (Exception e)
		{
			System.err.println("[ERROR] 关闭 Netty 出错：" + e.getMessage());
			// e.printStackTrace();
		} finally
		{
			started.set(false);
			System.out.println("[INFO] Netty 服务已关闭");
		}
	}

	/**
	 * 弹出桌面错误提示框
	 */
	private void showError(String title, Exception e)
	{
		// System.err.println("[ERROR] " + title + "：" + e.getMessage());
		// e.printStackTrace();

		JOptionPane.showMessageDialog(
				null,
				title + "\n" + e.getMessage(),
				"启动失败",
				JOptionPane.ERROR_MESSAGE
		);

		disable(); // 尝试安全关闭
	}
}
