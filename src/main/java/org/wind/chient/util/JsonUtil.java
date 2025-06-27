package org.wind.chient.util;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.wind.util.Config;

import java.util.List;

public class JsonUtil
{
	/**
	 * 解析成 JSON 数据
	 *
	 * @param bytes 消息字节
	 */
	public static DocumentContext parse(byte[] bytes)
	{
		// 转文本
		String msgText = new String(bytes);
		// 按行分割消息
		String[] msgTextLines = msgText.split("\r\n");
		// 按空格分割第一行
		String[] msgTextLine1 = msgTextLines[0].split(" ");
		// 按数组顺序获取第一行数据
		String method = msgTextLine1[0]; // 方法
		String uri = msgTextLine1[1]; // 完整请求地址
		String version = msgTextLine1[2]; // 请求版本
		// 循环每行消息，找出以 "Host: " 为首的行，并分割出 host 地址
		String hostHeader = "";
		for (int i = 1; i < msgTextLines.length; i++)
		{
			if (msgTextLines[i].startsWith("Host: "))
			{
				hostHeader = msgTextLines[i].substring("Host: ".length());
				break;
			}
		}
		// 判断 方法 == CONNECT
		boolean isConnect = "CONNECT".equalsIgnoreCase(method);
		// 决定用 uri 还是 hostHeader 当作真实 host
		String host = isConnect ? uri : hostHeader;
		// 端口
		int port;
		// 按 : 分割 host
		String[] hostParts = host.split(":");
		// 如果 hostParts 长度大于 0，数组第一位就是 host
		if (hostParts.length > 0)
		{
			host = hostParts[0];
		}
		// 如果 hostParts 长度大于 1，数组第二位就是 port
		if (hostParts.length > 1)
		{
			port = safeParsePort(hostParts[1], isConnect ? 443 : 80);
		} else
		{
			port = isConnect ? 443 : 80;
		}
		// 读取直连地址列表
		List<String> directList = Config.data.read("$.direct");
		// 循环判断 host == 直连
		boolean isDirect = false;
		for (String dl : directList)
		{
			isDirect = host.contains(dl);
			if (isDirect) break;
		}
		// 如果 host != 直连，修改 host 和 port 为代理服务器
		if (!isDirect)
		{
			host = Config.data.read("$.host");
			port = Config.data.read("$.port");
		}

		// 使用 StringBuffer 构建 JSON数据并返回
		StringBuffer jsonBuffer = new StringBuffer();
		// JSON 对象开始
		jsonBuffer.append("{");
		jsonBuffer.append("\"host\":").append("\"").append(host).append("\",");
		jsonBuffer.append("\"port\":").append(port).append(",");
		jsonBuffer.append("\"isDirect\":").append(isDirect).append(",");
		jsonBuffer.append("\"isConnect\":").append(isConnect).append(",");
		jsonBuffer.append("\"established\":").append("\"").append(version).append(" 200 Connection Established\r\n\r\n").append("\"");
		// JSON 对象结束
		jsonBuffer.append("}");
		return JsonPath.parse(jsonBuffer.toString());
	}

	/**
	 * 安全解析端口号，非法时返回默认值
	 */
	private static int safeParsePort(String str, int defaultPort)
	{
		try
		{
			return Integer.parseInt(str);
		} catch (NumberFormatException e)
		{
			return defaultPort;
		}
	}
}
