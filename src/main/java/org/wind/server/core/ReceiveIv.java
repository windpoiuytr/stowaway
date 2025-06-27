package org.wind.server.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.wind.util.AESCtrCipherUtil;
import org.wind.util.Config;

import java.security.GeneralSecurityException;

public class ReceiveIv extends ChannelInboundHandlerAdapter
{
	// 定义属性的键（Key）
	static final AttributeKey<AESCtrCipherUtil> CIPHER_UTIL_KEY = AttributeKey.valueOf("CipherUtilKey");

	/**
	 * 管道读
	 *
	 * @param ctx 管道处理程序上下文
	 * @param msg 管道消息
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws GeneralSecurityException
	{
		// 转字节
		byte[] iv = AESCtrCipherUtil.toBytes(msg);

		// 创建加解密工具类，并保存至管道上下文
		AESCtrCipherUtil cipherUtil = new AESCtrCipherUtil(Config.data.read("$.sk", String.class).getBytes(), iv);
		ctx.channel().attr(CIPHER_UTIL_KEY).set(cipherUtil);

		// 删除处理器
		ctx.channel().pipeline().remove(this);
		ctx.channel().pipeline().remove("ReceiveIvFixedLength");

		// 返回 OK
		ctx.channel().writeAndFlush(Unpooled.wrappedBuffer("OK".getBytes()));
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
