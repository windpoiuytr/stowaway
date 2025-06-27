package org.wind.chient.util;

import java.io.IOException;

public class SystemProxy
{
	/**
	 * 启用系统代理
	 *
	 * @param port 代理端口
	 */
	public static void enable(int port) throws IOException
	{
		if (System.getProperty("os.name").toLowerCase().contains("windows"))
		{
			Runtime.getRuntime().exec("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 1 /f");
			Runtime.getRuntime().exec(String.format("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /d \"127.0.0.1:%s\" /f", port));
			// Runtime.getRuntime().exec("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyOverride /t REG_SZ /d \"<local>;192.168.*\" /f");
		}
	}

	/**
	 * 禁用系统代理
	 */
	public static void disable()
	{
		if (System.getProperty("os.name").toLowerCase().contains("windows"))
		{
			try
			{
				Runtime.getRuntime().exec("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 0 /f");
			} catch (IOException e)
			{
				// 弹出桌面提示框
				javax.swing.JOptionPane.showMessageDialog(
						null,
						e.getMessage(),
						"禁用系统代理失败",
						javax.swing.JOptionPane.ERROR_MESSAGE
				);
			}
		}
	}
}
