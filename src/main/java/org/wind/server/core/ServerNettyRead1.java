package org.wind.server.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import org.wind.server.util.JsonUtil;
import org.wind.util.AESCtrCipherUtil;

public class ServerNettyRead1 extends ChannelInboundHandlerAdapter
{
	// 定义属性的键（Key）
	static final AttributeKey<Channel> CHANNEL_KEY = AttributeKey.valueOf("channelKey");
	static final AttributeKey<DocumentContext> JSON_KEY = AttributeKey.valueOf("jsonKey");
	private Channel channelClient;
	private Bootstrap bootstrap;
	private AESCtrCipherUtil cipherUtil;

	/**
	 * 管道激活
	 *
	 * @param ctx 管道处理程序上下文
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx)
	{
		channelClient = ctx.channel();
		bootstrap = new Bootstrap();
		bootstrap
				.group(channelClient.eventLoop())
				.channel(channelClient.getClass())
				.handler(new ChannelInitializer<SocketChannel>()
				{
					@Override
					protected void initChannel(SocketChannel socketChannel)
					{
						socketChannel.pipeline().addLast(new ServerNettyRead3());
					}
				});
	}

	/**
	 * 管道读
	 *
	 * @param ctx 管道处理程序上下文
	 * @param msg 管道消息
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		// 获取加解密工具类
		cipherUtil = channelClient.attr(ReceiveIv.CIPHER_UTIL_KEY).get();

		// 消息转字节并解密
		byte[] decryptBytes = cipherUtil.de(AESCtrCipherUtil.toBytes(msg));
		// 解析成 JSON 数据
		DocumentContext json = JsonUtil.parse(decryptBytes);

		// 连接服务端
		ChannelFuture channelFuture = bootstrap.connect(json.read("$.host", String.class), json.read("$.port", Integer.class));
		channelFuture.addListener((future) ->
		{
			// 连接成功
			if (future.isSuccess())
			{
				// 添加下一个处理器
				channelClient.pipeline().addLast(new ServerNettyRead2());

				// 服务端管道
				Channel channelServer = channelFuture.channel();

				// 存储到管道上下文
				channelClient.attr(CHANNEL_KEY).set(channelServer);
				channelClient.attr(JSON_KEY).set(json);
				channelServer.attr(CHANNEL_KEY).set(channelClient);
				channelServer.attr(JSON_KEY).set(json);
				// 保存加解密工具类至管道上下文
				channelServer.attr(ReceiveIv.CIPHER_UTIL_KEY).set(cipherUtil);

				// 如果是禁连
				if (json.read("$.isProhibit", Boolean.class))
				{
					// 关闭相关管道
					channelClient.close();
					channelServer.close();
				}
				// 如果是建立连接请求
				else if (json.read("$.isConnect", Boolean.class))
				{
					// 建立连接成功
					channelClient.writeAndFlush(Unpooled.wrappedBuffer(cipherUtil.en(json.read("$.established", String.class).getBytes())));
				}
				// 否则是代理
				else
				{
					channelServer.writeAndFlush(Unpooled.wrappedBuffer(decryptBytes));
				}
			}
		});

		// 删除处理器（自己）
		channelClient.pipeline().remove(this);
	}

	/**
	 * 管道未激活（关闭）
	 *
	 * @param ctx 管道处理程序上下文
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		ctx.close();
	}

	/**
	 * 管道异常已捕获
	 *
	 * @param ctx   管道处理程序上下文
	 * @param cause 异常信息
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		ctx.close();
	}
}
