package org.wind.server.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.wind.util.AESCtrCipherUtil;

public class ServerNettyRead3 extends ChannelInboundHandlerAdapter
{
	/**
	 * 管道读
	 *
	 * @param ctx 管道处理程序上下文
	 * @param msg 管道消息
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		// 获取管道上下文存储的数据
		Channel channelClient = ctx.channel().attr(ServerNettyRead1.CHANNEL_KEY).get();
		DocumentContext json = ctx.channel().attr(ServerNettyRead1.JSON_KEY).get();
		AESCtrCipherUtil cipherUtil = ctx.channel().attr(ReceiveIv.CIPHER_UTIL_KEY).get();

		// 如果是禁连
		if (json.read("$.isProhibit", Boolean.class))
		{
			// 关闭相关管道
			ctx.channel().close();
			channelClient.close();
		}
		// 否则是代理
		else
		{
			channelClient.writeAndFlush(Unpooled.wrappedBuffer(cipherUtil.en(AESCtrCipherUtil.toBytes(msg))));
		}
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
