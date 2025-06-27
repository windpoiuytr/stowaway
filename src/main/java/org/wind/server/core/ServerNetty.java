package org.wind.server.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

public class ServerNetty
{
	private final int port;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel serverChannel;

	public ServerNetty(int port)
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
						socketChannel.pipeline()
								.addLast("ReceiveIvFixedLength", new FixedLengthFrameDecoder(16))
								.addLast(new ReceiveIv())
								.addLast(new ServerNettyRead1());
					}
				});

		try
		{
			ChannelFuture future = serverBootstrap.bind(port).sync();
			serverChannel = future.channel(); // 保存引用，供关闭时使用
			System.out.println("Netty 启动成功，端口：" + port);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			disable();
		}
	}

	/**
	 * 禁用 Netty 服务
	 */
	public void disable()
	{
		// 优雅关闭通道
		if (serverChannel != null) serverChannel.close();
		if (bossGroup != null) bossGroup.shutdownGracefully();
		if (workerGroup != null) workerGroup.shutdownGracefully();

		System.out.println("Netty 服务器已关闭");
	}
}
