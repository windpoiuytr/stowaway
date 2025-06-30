package org.wind.chient.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.wind.util.AESCtrCipherUtil;

public class ClientNettyRead3 extends ChannelInboundHandlerAdapter
{

	/**
	 * 接收到远程服务器返回的数据，转发给客户端（浏览器）
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try
		{
			// 当前为服务端通道
			Channel channelServer = ctx.channel();

			// 获取客户端通道、JSON 配置、加解密工具
			Channel channelClient = channelServer.attr(ClientNettyRead1.CHANNEL_KEY).get();
			DocumentContext json = channelServer.attr(ClientNettyRead1.JSON_KEY).get();

			// 防御性检查
			if (channelClient == null || json == null)
			{
				// System.err.println("[WARN] channelRead3: 上下文缺失，连接异常");
				channelServer.close();
				return;
			}

			// 获取原始数据
			byte[] data = AESCtrCipherUtil.toBytes(msg);

			// 判断是否是直连模式
			boolean isDirect = json.read("$.isDirect", Boolean.class);

			// 获取处理后要写回客户端的数据
			byte[] output = isDirect
					? data
					: channelServer.attr(ReceiveOk.CIPHER_UTIL_KEY).get().de(data);

			// 写回客户端
			channelClient.writeAndFlush(Unpooled.wrappedBuffer(output));

		} catch (Exception e)
		{
			// System.err.println("[ERROR] channelRead3: 数据回传失败：" + e.getMessage());
			// e.printStackTrace();
			ctx.close();
		}
	}

	/**
	 * 当前通道关闭时，关闭对端连接（客户端）
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
		// System.out.println("[INFO] channelInactive3: 服务端通道断开，对端也已关闭");
	}

	/**
	 * 异常捕获时，关闭自己和对端
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// System.err.println("[ERROR] 服务端返回数据异常：" + cause.getMessage());
		// cause.printStackTrace();

		Channel peer = ctx.channel().attr(ClientNettyRead1.CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}
		ctx.close();
	}
}
