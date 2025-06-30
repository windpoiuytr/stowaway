package org.wind.server.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.wind.util.AESCtrCipherUtil;
import org.wind.util.Config;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class ReceiveIv extends ChannelInboundHandlerAdapter
{
	// 定义属性的键（Key）
	static final AttributeKey<AESCtrCipherUtil> CIPHER_UTIL_KEY = AttributeKey.valueOf("CipherUtilKey");

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try
		{
			// 转字节，校验长度
			byte[] iv = AESCtrCipherUtil.toBytes(msg);
			if (iv == null || iv.length != 16)
			{
				// System.err.println("[WARN] ReceiveIv: IV 长度非法，收到长度：" + (iv == null ? "null" : iv.length));
				ctx.close();
				return;
			}

			// 创建加解密工具类
			byte[] key = Config.data.read("$.sk", String.class).getBytes(StandardCharsets.UTF_8);
			AESCtrCipherUtil cipherUtil = new AESCtrCipherUtil(key, iv);
			ctx.channel().attr(CIPHER_UTIL_KEY).set(cipherUtil);

			// 删除当前和定长解码处理器，非空校验防止异常
			if (ctx.channel().pipeline().get(this.getClass()) != null)
			{
				ctx.channel().pipeline().remove(this);
			}
			if (ctx.channel().pipeline().get("ReceiveIvFixedLength") != null)
			{
				ctx.channel().pipeline().remove("ReceiveIvFixedLength");
			}

			// 返回握手确认
			ctx.channel().writeAndFlush(Unpooled.wrappedBuffer("OK".getBytes(StandardCharsets.UTF_8)));
			// System.out.println("[INFO] ReceiveIv: 握手完成，IV设置成功");
		} catch (GeneralSecurityException e)
		{
			// System.err.println("[ERROR] ReceiveIv: 加解密工具初始化失败：" + e.getMessage());
			// e.printStackTrace();
			ctx.close();
		} catch (Exception e)
		{
			// System.err.println("[ERROR] ReceiveIv: 未知异常：" + e.getMessage());
			// e.printStackTrace();
			ctx.close();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		// System.out.println("[INFO] ReceiveIv: 通道关闭 -> " + ctx.channel());
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// System.err.println("[ERROR] ReceiveIv 捕获异常：" + cause.getMessage());
		// cause.printStackTrace();
		ctx.close();
	}
}
