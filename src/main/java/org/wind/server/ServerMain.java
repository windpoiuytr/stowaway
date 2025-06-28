package org.wind.server;

import org.wind.server.core.ServerNetty;
import org.wind.util.Config;

public class ServerMain
{
	public static void main(String[] args)
	{
		try
		{
			// 获取配置文件
			Config.get();
		} catch (Exception e)
		{
			throw new RuntimeException("获取配置文件失败", e);
		}

		// 新建 Netty 服务
		ServerNetty serverNetty = new ServerNetty(Config.data.read("$.port", Integer.class));
		serverNetty.enable();

		// 添加关闭钩子（确保系统关机/重启时清理）
		Runtime.getRuntime().addShutdownHook(new Thread(serverNetty::disable));
	}
}