package org.wind.chient.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.wind.chient.util.SystemProxy;

import java.io.IOException;

public class ClientNetty
{
	private final int port;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel serverChannel;

	public ClientNetty(int port)
	{
		this.port = port;
	}

	/**
	 * 启用 Netty 服务
	 */
	public void enable()
	{
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();

		ServerBootstrap serverBootstrap = new ServerBootstrap();
		serverBootstrap
				.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childOption(ChannelOption.TCP_NODELAY, true) // 关闭 Nagle 算法，低延迟
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) // 使用堆外内存池
				.childHandler(new ChannelInitializer<SocketChannel>()
				{
					@Override
					protected void initChannel(SocketChannel socketChannel)
					{
						socketChannel.pipeline().addLast(new ClientNettyRead1());
					}
				});

		try
		{
			ChannelFuture future = serverBootstrap.bind(port).sync();
			serverChannel = future.channel(); // 保存引用，供关闭时使用
			System.out.println("Netty 启动成功，端口：" + port);
			// 启用系统代理
			SystemProxy.enable(port);
		} catch (InterruptedException | IOException e)
		{
			// 弹出桌面提示框
			javax.swing.JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					"Netty 启动失败",
					javax.swing.JOptionPane.ERROR_MESSAGE
			);
			disable();
		}
	}

	/**
	 * 禁用 Netty 服务
	 */
	public void disable()
	{
		// 禁用系统代理
		SystemProxy.disable();

		// 优雅关闭通道
		if (serverChannel != null) serverChannel.close();
		if (bossGroup != null) bossGroup.shutdownGracefully().syncUninterruptibly();
		if (workerGroup != null) workerGroup.shutdownGracefully().syncUninterruptibly();

		System.out.println("Netty 服务器已关闭");
	}
}
