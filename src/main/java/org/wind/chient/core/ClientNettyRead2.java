package org.wind.chient.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.wind.util.AESCtrCipherUtil;

public class ClientNettyRead2 extends ChannelInboundHandlerAdapter
{

	/**
	 * 接收到来自客户端的数据，进行转发（直连或加密）
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try
		{
			// 获取当前 Channel（客户端） 和对端 Channel（服务端）
			Channel channelClient = ctx.channel();
			Channel channelServer = channelClient.attr(ClientNettyRead1.CHANNEL_KEY).get();
			DocumentContext json = channelClient.attr(ClientNettyRead1.JSON_KEY).get();

			// 防御性编程，确保属性存在
			if (channelServer == null || json == null)
			{
				// System.err.println("[WARN] channelRead 时上下文信息缺失，可能连接已失效");
				channelClient.close();
				return;
			}

			// 读取数据
			byte[] data = AESCtrCipherUtil.toBytes(msg);

			// 判断是否为直连模式
			boolean isDirect = json.read("$.isDirect", Boolean.class);

			// 加密模式下需获取加密工具
			byte[] out = isDirect
					? data
					: channelClient.attr(ReceiveOk.CIPHER_UTIL_KEY).get().en(data);

			// 写入对端服务端通道
			channelServer.writeAndFlush(Unpooled.wrappedBuffer(out));

		} catch (Exception e)
		{
			// System.err.println("[ERROR] channelRead 转发出错：" + e.getMessage());
			// e.printStackTrace();
			ctx.close(); // 出错后关闭连接
		}
	}

	/**
	 * 管道关闭时，关闭对端连接，防止资源泄漏
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
		// System.out.println("[INFO] channelInactive: 已关闭连接和对端");
	}

	/**
	 * 异常捕获时，关闭当前与对端连接，并记录日志
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// System.err.println("[ERROR] 连接异常：" + cause.getMessage());
		// cause.printStackTrace();

		Channel peer = ctx.channel().attr(ClientNettyRead1.CHANNEL_KEY).get();
		if (peer != null && peer.isOpen())
		{
			peer.close();
		}
		ctx.close();
	}
}
