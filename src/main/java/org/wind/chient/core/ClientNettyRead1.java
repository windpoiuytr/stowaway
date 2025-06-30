package org.wind.chient.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.util.AttributeKey;
import org.wind.chient.util.JsonUtil;
import org.wind.util.AESCtrCipherUtil;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientNettyRead1 extends ChannelInboundHandlerAdapter
{
	// 属性键，用于在 Channel 上存储对端 Channel 和 JSON 信息
	static final AttributeKey<Channel> CHANNEL_KEY = AttributeKey.valueOf("channelKey");
	static final AttributeKey<DocumentContext> JSON_KEY = AttributeKey.valueOf("jsonKey");
	// 可选：连接统计器（便于调试）
	private static final AtomicInteger connectionCounter = new AtomicInteger(0);
	private Channel channelClient;  // 当前客户端 Channel
	private Bootstrap bootstrap;    // 用于连接远程服务端

	/**
	 * 管道激活时初始化 Bootstrap
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx)
	{
		channelClient = ctx.channel();

		bootstrap = new Bootstrap();
		bootstrap
				.group(channelClient.eventLoop()) // 使用同一个线程池
				.channel(channelClient.getClass()) // 同类 channel
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.handler(new ChannelInitializer<SocketChannel>()
				{
					@Override
					protected void initChannel(SocketChannel socketChannel)
					{
						socketChannel.pipeline().addLast(new ClientNettyRead3());
					}
				});

		// 打印调试信息
		int current = connectionCounter.incrementAndGet();
		// System.out.println("[DEBUG] 激活连接数 +1 => " + current);
	}

	/**
	 * 接收客户端发来的 JSON 数据，解析后连接服务端
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try
		{
			// 将接收到的对象转换为字节数组
			byte[] bytes = AESCtrCipherUtil.toBytes(msg);

			// 解析 JSON 格式
			DocumentContext json = JsonUtil.parse(bytes);

			// 获取目标主机和端口
			String host = json.read("$.host", String.class);
			int port = json.read("$.port", Integer.class);

			// 发起连接
			ChannelFuture channelFuture = bootstrap.connect(host, port);
			channelFuture.addListener((ChannelFuture future) ->
			{
				if (future.isSuccess())
				{
					// 连接成功，获取服务端 Channel
					Channel channelServer = future.channel();

					// 将对端绑定到彼此
					channelClient.attr(CHANNEL_KEY).set(channelServer);
					channelClient.attr(JSON_KEY).set(json);
					channelServer.attr(CHANNEL_KEY).set(channelClient);
					channelServer.attr(JSON_KEY).set(json);

					// 添加下一阶段处理器
					channelClient.pipeline().addLast(new ClientNettyRead2());

					// 处理直连情况
					if (json.read("$.isDirect", Boolean.class))
					{
						if (json.read("$.isConnect", Boolean.class))
						{
							// 客户端发起的是 CONNECT 请求，返回已连接响应
							String response = json.read("$.established", String.class);
							channelClient.writeAndFlush(Unpooled.wrappedBuffer(response.getBytes()));
						} else
						{
							// 直连数据转发
							channelServer.writeAndFlush(Unpooled.wrappedBuffer(bytes));
						}
					}
					// 代理加密情况
					else
					{
						// 生成 16 字节随机 IV（128位）
						byte[] iv = new byte[16];
						new SecureRandom().nextBytes(iv);

						// 给服务端 pipeline 添加解码和响应处理器
						channelServer.pipeline()
								.addFirst(new ReceiveOk(iv, bytes))
								.addFirst("ReceiveOkFixedLength", new FixedLengthFrameDecoder(2));

						// 将 IV 发送给远端服务
						channelServer.writeAndFlush(Unpooled.wrappedBuffer(iv));
					}

					// 监听两端关闭，互相关闭彼此
					addCloseListeners(channelClient, channelServer);

				} else
				{
					// 连接失败时关闭当前连接并打印原因
					// System.err.println("[ERROR] 连接目标失败: " + host + ":" + port);
					// future.cause().printStackTrace();
					ctx.close();
				}
			});

		} catch (Exception e)
		{
			// e.printStackTrace();
			ctx.close();
		} finally
		{
			// 移除自身处理器，避免重复读取
			ctx.pipeline().remove(this);
		}
	}

	/**
	 * 双向添加关闭监听器：一端断开，另一端也关闭
	 */
	private void addCloseListeners(Channel channel1, Channel channel2)
	{
		channel1.closeFuture().addListener(future ->
		{
			if (channel2.isOpen()) channel2.close();
		});
		channel2.closeFuture().addListener(future ->
		{
			if (channel1.isOpen()) channel1.close();
		});
	}

	/**
	 * 管道关闭时，清理连接并统计
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		// 获取对端 Channel，关闭
		Channel peer = ctx.channel().attr(CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}

		// 减少连接计数
		int current = connectionCounter.decrementAndGet();
		// System.out.println("[DEBUG] 连接断开 -1 => " + current);
	}

	/**
	 * 异常处理时关闭两端连接
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// 打印异常信息
		// System.err.println("[ERROR] 异常捕获: " + cause.getMessage());
		// cause.printStackTrace();

		// 获取并关闭对端 Channel
		Channel peer = ctx.channel().attr(CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}

		// 自己也关闭
		ctx.close();
	}
}
