package org.wind.chient.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.wind.util.AESCtrCipherUtil;
import org.wind.util.Config;

public class ReceiveOk extends ChannelInboundHandlerAdapter
{

	// 保存加密工具的通道属性键
	public static final AttributeKey<AESCtrCipherUtil> CIPHER_UTIL_KEY = AttributeKey.valueOf("CipherUtilKey");

	private final byte[] iv;     // 客户端生成的随机 IV
	private final byte[] bytes;  // 客户端发起连接的加密数据（待发送）

	public ReceiveOk(byte[] iv, byte[] bytes)
	{
		this.iv = iv;
		this.bytes = bytes;
	}

	/**
	 * 接收服务端的 OK 响应
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try
		{
			// 解码接收到的内容
			byte[] responseBytes = AESCtrCipherUtil.toBytes(msg);
			String response = new String(responseBytes).trim();

			if (!"OK".equals(response))
			{
				// System.err.println("[ERROR] 握手响应非 OK，实际返回：" + response);
				ctx.close();
				return;
			}

			// 获取客户端通道引用
			Channel channelClient = ctx.channel().attr(ClientNettyRead1.CHANNEL_KEY).get();
			if (channelClient == null)
			{
				// System.err.println("[ERROR] 握手成功，但未获取到客户端通道");
				ctx.close();
				return;
			}

			// 生成加解密工具类，并保存到服务端和客户端通道中
			byte[] key = Config.data.read("$.sk", String.class).getBytes();
			AESCtrCipherUtil cipherUtil = new AESCtrCipherUtil(key, iv);
			ctx.channel().attr(CIPHER_UTIL_KEY).set(cipherUtil);
			channelClient.attr(CIPHER_UTIL_KEY).set(cipherUtil);

			// 删除自身和定长帧处理器
			ctx.pipeline().remove(this);
			ctx.pipeline().remove("ReceiveOkFixedLength");

			// 将原始请求密文再次发送出去
			ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(cipherUtil.en(bytes)));

		} catch (Exception e)
		{
			// System.err.println("[ERROR] 握手处理异常：" + e.getMessage());
			// e.printStackTrace();
			ctx.close();
		}
	}

	/**
	 * 通道关闭，联动关闭对端通道
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		Channel peer = ctx.channel().attr(ClientNettyRead1.CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}
		ctx.close();
		// System.out.println("[INFO] ReceiveOk: 通道关闭，已联动关闭对端");
	}

	/**
	 * 捕获异常时关闭通道并打印日志
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// System.err.println("[ERROR] ReceiveOk 异常：" + cause.getMessage());
		// cause.printStackTrace();

		Channel peer = ctx.channel().attr(ClientNettyRead1.CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}
		ctx.close();
	}
}
