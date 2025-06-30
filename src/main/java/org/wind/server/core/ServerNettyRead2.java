package org.wind.server.core;

import com.jayway.jsonpath.DocumentContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.wind.util.AESCtrCipherUtil;

public class ServerNettyRead2 extends ChannelInboundHandlerAdapter
{

	/**
	 * 读取客户端发来的数据，解密后转发给目标服务器通道；
	 * 如果连接被禁止，则关闭当前通道及关联通道。
	 *
	 * @param ctx 管道上下文，包含当前通道等信息
	 * @param msg 收到的消息对象，通常是字节缓冲
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		// 获取当前通道（客户端通道）
		Channel currentChannel = ctx.channel();

		try
		{
			// 从当前通道属性中获取关联的目标服务器通道
			Channel channelPeer = currentChannel.attr(ServerNettyRead1.CHANNEL_KEY).get();

			// 从当前通道属性中获取配置的 JSON 对象
			DocumentContext json = currentChannel.attr(ServerNettyRead1.JSON_KEY).get();

			// 从当前通道属性中获取加解密工具实例
			AESCtrCipherUtil cipherUtil = currentChannel.attr(ReceiveIv.CIPHER_UTIL_KEY).get();

			// 防御性检查：任何必要对象为空，则关闭当前通道避免异常
			if (channelPeer == null || json == null || cipherUtil == null)
			{
				// System.err.println("[WARN] ServerNettyRead2: 关联通道或必要数据为空，关闭当前通道");
				safeClose(currentChannel);
				return;
			}

			// 读取配置字段，判断连接是否被禁止
			boolean isProhibit = false;
			try
			{
				Boolean val = json.read("$.isProhibit", Boolean.class);
				isProhibit = val != null && val;
			} catch (Exception e)
			{
				// System.err.println("[WARN] ServerNettyRead2: 读取 isProhibit 字段异常，默认不禁止连接");
			}

			// 如果连接被禁止，关闭当前通道和目标服务器通道，停止转发
			if (isProhibit)
			{
				// System.out.println("[INFO] ServerNettyRead2: 连接被禁止，关闭当前通道及关联通道");
				safeClose(currentChannel);
				safeClose(channelPeer);
				return;
			}

			// 将收到的消息转为字节数组
			byte[] msgBytes = AESCtrCipherUtil.toBytes(msg);

			// 解密数据
			byte[] decrypted = cipherUtil.de(msgBytes);

			// 将解密后的数据写入目标服务器通道，完成转发
			channelPeer.writeAndFlush(Unpooled.wrappedBuffer(decrypted));

		} catch (Exception e)
		{
			// 出现异常时打印异常堆栈并关闭相关通道，防止资源泄漏
			// System.err.println("[ERROR] ServerNettyRead2: 解密或转发异常，关闭通道。异常信息：" + e.getMessage());
			// e.printStackTrace();

			safeClose(currentChannel);

			Channel channelPeer = currentChannel.attr(ServerNettyRead1.CHANNEL_KEY).get();
			safeClose(channelPeer);
		}
	}

	/**
	 * 通道关闭时，打印日志并安全关闭当前通道释放资源。
	 *
	 * @param ctx 管道上下文
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		// 获取当前通道
		Channel currentChannel = ctx.channel();

		// System.out.println("[INFO] ServerNettyRead2: 通道关闭 -> " + currentChannel);

		// 关闭当前通道
		safeClose(currentChannel);
	}

	/**
	 * 异常捕获时，打印异常堆栈并关闭当前通道，避免异常扩散。
	 *
	 * @param ctx   管道上下文
	 * @param cause 异常对象
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// 获取当前通道
		Channel currentChannel = ctx.channel();

		// 打印错误信息和异常堆栈
		// System.err.println("[ERROR] ServerNettyRead2 捕获异常：" + cause.getMessage());
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
