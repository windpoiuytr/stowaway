package org.wind.chient.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.wind.util.AESCtrCipherUtil;
import org.wind.util.Config;

import java.security.GeneralSecurityException;

public class ReceiveOk extends ChannelInboundHandlerAdapter
{
	// 定义属性的键（Key）
	static final AttributeKey<AESCtrCipherUtil> CIPHER_UTIL_KEY = AttributeKey.valueOf("CipherUtilKey");
	private final byte[] iv;
	private final byte[] bytes;

	public ReceiveOk(byte[] iv, byte[] bytes)
	{
		this.iv = iv;
		this.bytes = bytes;
	}

	/**
	 * 管道读
	 *
	 * @param ctx 管道处理程序上下文
	 * @param msg 管道消息
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws GeneralSecurityException
	{
		// 握手成功
		if ("OK".equals(new String(AESCtrCipherUtil.toBytes(msg))))
		{
			// 获取客户端管道
			Channel channelClient = ctx.channel().attr(ClientNettyRead1.CHANNEL_KEY).get();

			// 创建加解密工具类，并保存至管道上下文
			AESCtrCipherUtil cipherUtil = new AESCtrCipherUtil(Config.data.read("$.sk", String.class).getBytes(), iv);
			ctx.channel().attr(CIPHER_UTIL_KEY).set(cipherUtil);
			channelClient.attr(CIPHER_UTIL_KEY).set(cipherUtil);

			// 删除处理器
			ctx.channel().pipeline().remove(this);
			ctx.channel().pipeline().remove("ReceiveOkFixedLength");

			// 发送密文至服务端
			ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(cipherUtil.en(bytes)));
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
