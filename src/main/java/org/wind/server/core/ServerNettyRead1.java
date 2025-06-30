package org.wind.server.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import org.wind.server.util.JsonUtil;
import org.wind.util.AESCtrCipherUtil;

public class ServerNettyRead1 extends ChannelInboundHandlerAdapter
{

	static final AttributeKey<Channel> CHANNEL_KEY = AttributeKey.valueOf("channelKey");
	static final AttributeKey<DocumentContext> JSON_KEY = AttributeKey.valueOf("jsonKey");

	private final Bootstrap bootstrap;

	public ServerNettyRead1()
	{
		bootstrap = new Bootstrap();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		bootstrap.group(ctx.channel().eventLoop())
				.channel(ctx.channel().getClass())
				.handler(new ChannelInitializer<SocketChannel>()
				{
					@Override
					protected void initChannel(SocketChannel ch)
					{
						ch.pipeline().addLast(new ServerNettyRead3());
					}
				});
		super.channelActive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		Channel currentChannel = ctx.channel();

		// 取加解密工具
		AESCtrCipherUtil cipherUtil = currentChannel.attr(ReceiveIv.CIPHER_UTIL_KEY).get();
		if (cipherUtil == null)
		{
			// System.err.println("[ERROR] ServerNettyRead1: 未找到加解密工具，关闭连接");
			safeClose(currentChannel);
			return;
		}

		try
		{
			byte[] decryptBytes = cipherUtil.de(AESCtrCipherUtil.toBytes(msg));
			DocumentContext json = JsonUtil.parse(decryptBytes);

			String host = json.read("$.host", String.class);
			Integer port = json.read("$.port", Integer.class);
			if (host == null || port == null)
			{
				// System.err.println("[ERROR] ServerNettyRead1: JSON中host或port为空，关闭连接");
				safeClose(currentChannel);
				return;
			}

			ChannelFuture future = bootstrap.connect(host, port);
			future.addListener((ChannelFuture f) ->
			{
				if (f.isSuccess())
				{
					Channel remoteChannel = f.channel();

					// 互相保存对端通道
					currentChannel.attr(CHANNEL_KEY).set(remoteChannel);
					remoteChannel.attr(CHANNEL_KEY).set(currentChannel);

					// 保存JSON和加密工具到两端
					currentChannel.attr(JSON_KEY).set(json);
					remoteChannel.attr(JSON_KEY).set(json);
					remoteChannel.attr(ReceiveIv.CIPHER_UTIL_KEY).set(cipherUtil);

					// 双向关闭监听器：一端断开另一端也关闭
					addCloseListeners(currentChannel, remoteChannel);

					currentChannel.pipeline().addLast(new ServerNettyRead2());

					if (Boolean.TRUE.equals(json.read("$.isProhibit", Boolean.class)))
					{
						// System.out.println("[INFO] ServerNettyRead1: 连接被禁止，关闭双方通道");
						safeClose(currentChannel);
						safeClose(remoteChannel);
					} else if (Boolean.TRUE.equals(json.read("$.isConnect", Boolean.class)))
					{
						byte[] established = cipherUtil.en(json.read("$.established", String.class).getBytes());
						currentChannel.writeAndFlush(Unpooled.wrappedBuffer(established));
					} else
					{
						remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(decryptBytes));
					}

					currentChannel.pipeline().remove(ServerNettyRead1.this);

				} else
				{
					// System.err.printf("[ERROR] ServerNettyRead1: 连接目标 %s:%d 失败: %s%n", host, port, f.cause());
					safeClose(currentChannel);
				}
			});
		} catch (Exception e)
		{
			// System.err.println("[ERROR] ServerNettyRead1: 处理消息异常：" + e.getMessage());
			// e.printStackTrace();
			safeClose(currentChannel);
		}
	}

	/**
	 * 双向添加关闭监听器，确保一端关闭时另一端也关闭，避免资源泄漏
	 *
	 * @param channel1 通道1
	 * @param channel2 通道2
	 */
	private void addCloseListeners(Channel channel1, Channel channel2)
	{
		channel1.closeFuture().addListener(future ->
		{
			if (channel2.isOpen())
			{
				channel2.close();
			}
		});
		channel2.closeFuture().addListener(future ->
		{
			if (channel1.isOpen())
			{
				channel1.close();
			}
		});
	}

	/**
	 * 通道关闭时，同时关闭对端连接，防止资源泄漏
	 *
	 * @param ctx 管道处理程序上下文
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		Channel peer = ctx.channel().attr(CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}
		ctx.close();
		// System.out.println("[INFO] ServerNettyRead1: 通道关闭，已关闭对端通道");
	}

	/**
	 * 异常捕获时，关闭自身和对端连接，防止资源泄漏，并打印日志
	 *
	 * @param ctx   管道处理程序上下文
	 * @param cause 异常信息
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// System.err.println("[ERROR] ServerNettyRead1: 异常捕获 - " + cause.getMessage());
		// cause.printStackTrace();
		Channel peer = ctx.channel().attr(CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}
		ctx.close();
	}

	/**
	 * 安全关闭通道，避免空指针和重复关闭
	 *
	 * @param channel 目标通道
	 */
	private void safeClose(Channel channel)
	{
		if (channel != null && channel.isOpen())
		{
			channel.close();
		}
	}
}
