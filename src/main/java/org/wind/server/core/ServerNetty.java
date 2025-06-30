package org.wind.server.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerNetty
{

	private final int port;
	private final AtomicBoolean started = new AtomicBoolean(false); // 防止重复启动
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel serverChannel;

	public ServerNetty(int port)
	{
		this.port = port;
	}

	/**
	 * 启动 Netty 服务端
	 */
	public void enable()
	{
		if (started.get())
		{
			// System.out.println("[WARN] 服务已启动，无需重复启动");
			return;
		}

		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();

		try
		{
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap
					.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_REUSEADDR, true) // 端口复用
					.childOption(ChannelOption.SO_KEEPALIVE, true) // 长连接
					.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.childHandler(new ChannelInitializer<SocketChannel>()
					{
						@Override
						protected void initChannel(SocketChannel socketChannel)
						{
							socketChannel.pipeline()
									.addLast("ReceiveIvFixedLength", new FixedLengthFrameDecoder(16))
									.addLast(new ReceiveIv())
									.addLast(new ServerNettyRead1());
						}
					});

			ChannelFuture future = bootstrap.bind(port).sync();
			serverChannel = future.channel();
			started.set(true);
			System.out.println("[INFO] Netty 服务端已启动，监听端口：" + port);

		} catch (InterruptedException e)
		{
			logErrorAndShutdown("Netty 启动被中断", e);
			Thread.currentThread().interrupt(); // 恢复中断状态
		} catch (Exception e)
		{
			logErrorAndShutdown("Netty 启动异常", e);
		}
	}

	/**
	 * 停止 Netty 服务端
	 */
	public void disable()
	{
		if (!started.get())
		{
			// System.out.println("[WARN] 服务未启动，无需关闭");
			return;
		}

		// System.out.println("[INFO] 正在关闭 Netty 服务端...");

		try
		{
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
			System.out.println("[INFO] Netty 服务端已关闭");

		} catch (Exception e)
		{
			System.err.println("[ERROR] 关闭 Netty 服务端时出错：" + e.getMessage());
			// e.printStackTrace();
		} finally
		{
			started.set(false);
		}
	}

	/**
	 * 控制台日志输出 + 关闭服务端
	 */
	private void logErrorAndShutdown(String title, Exception e)
	{
		// System.err.println("[ERROR] " + title + "：" + e.getMessage());
		// e.printStackTrace();
		disable();
	}
}
