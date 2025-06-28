package org.wind.util;

import java.security.SecureRandom;

public class PortSKGenerator
{
	private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom random = new SecureRandom();

	public static int generatePort()
	{
		// 从 49152~65535 随机选择
		return 49152 + random.nextInt(65535 - 49152 + 1);
	}

	public static String generateSK(int length)
	{
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
		{
			sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
		}
		return sb.toString();
	}
}
