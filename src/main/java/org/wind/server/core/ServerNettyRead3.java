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
	 * 处理接收到的服务端数据，进行加密后转发给客户端。
	 *
	 * @param ctx 管道上下文，包含当前通道等信息
	 * @param msg 收到的消息对象，通常是字节缓冲
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		// 获取当前通道（服务端通道）
		Channel currentChannel = ctx.channel();

		try
		{
			// 从当前通道属性中获取关联的客户端通道
			Channel channelClient = currentChannel.attr(ServerNettyRead1.CHANNEL_KEY).get();

			// 从当前通道属性中获取配置的 JSON 对象
			DocumentContext json = currentChannel.attr(ServerNettyRead1.JSON_KEY).get();

			// 从当前通道属性中获取加解密工具实例
			AESCtrCipherUtil cipherUtil = currentChannel.attr(ReceiveIv.CIPHER_UTIL_KEY).get();

			// 如果任一关键对象为空，打印错误并关闭当前通道
			if (channelClient == null || json == null || cipherUtil == null)
			{
				// System.err.println("[ERROR] ServerNettyRead3: 关联通道或必要数据为空，关闭当前通道");
				safeClose(currentChannel);
				return;
			}

			// 默认连接不禁止，准备检查 isProhibit 字段
			boolean isProhibit = false;
			try
			{
				// 读取是否禁止连接的标志
				Boolean val = json.read("$.isProhibit", Boolean.class);
				isProhibit = val != null && val;
			} catch (Exception e)
			{
				// 读取异常时打印警告，默认不禁止连接
				// System.err.println("[WARN] ServerNettyRead3: 读取 isProhibit 字段异常，默认不禁止连接");
			}

			// 如果连接被禁止，关闭当前通道和客户端通道
			if (isProhibit)
			{
				// System.out.println("[INFO] ServerNettyRead3: 连接被禁止，关闭当前通道及关联通道");
				safeClose(currentChannel);
				safeClose(channelClient);
				return;
			}

			// 将收到的消息转为字节数组
			byte[] msgBytes = AESCtrCipherUtil.toBytes(msg);

			// 加密数据
			byte[] encrypted = cipherUtil.en(msgBytes);

			// 将加密数据写回客户端通道
			channelClient.writeAndFlush(Unpooled.wrappedBuffer(encrypted));

		} catch (Exception e)
		{
			// 发生异常时打印错误和堆栈，关闭当前通道和客户端通道
			// System.err.println("[ERROR] ServerNettyRead3: 加密或转发异常，关闭通道。异常信息：" + e.getMessage());
			// e.printStackTrace();

			safeClose(currentChannel);
			Channel channelClient = currentChannel.attr(ServerNettyRead1.CHANNEL_KEY).get();
			safeClose(channelClient);
		}
	}

	/**
	 * 当前通道关闭时，安全关闭通道释放资源。
	 *
	 * @param ctx 管道上下文
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		// 获取当前通道
		Channel currentChannel = ctx.channel();

		// System.out.println("[INFO] ServerNettyRead3: 通道关闭 -> " + currentChannel);

		// 关闭当前通道
		safeClose(currentChannel);
	}

	/**
	 * 发生异常时，打印异常堆栈，关闭当前通道防止异常扩散。
	 *
	 * @param ctx   管道上下文
	 * @param cause 异常对象
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// 获取当前通道
		Channel currentChannel = ctx.channel();

		// 打印错误信息及异常堆栈
		// System.err.println("[ERROR] ServerNettyRead3 捕获异常：" + cause.getMessage());
		// cause.printStackTrace();

		// 关闭当前通道
		safeClose(currentChannel);
	}

	/**
	 * 安全关闭通道，避免空指针异常和重复关闭。
	 *
	 * @param channel 需要关闭的通道
	 */
	private void safeClose(Channel channel)
	{
		// 如果通道非空且处于打开状态，则关闭
		if (channel != null && channel.isOpen())
		{
			channel.close();
		}
	}
}
