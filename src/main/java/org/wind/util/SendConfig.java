package org.wind.util;

public class SendConfig
{
	public static void main(String[] args) throws Exception
	{
		int port = PortSKGenerator.generatePort();
		String sk = PortSKGenerator.generateSK(16);
		System.out.println(port + " - " + sk);

		// 配置
		String config = "{\n" +
				"  \"host\": \"45.205.28.11\",\n" +
				"  \"port\": " + port + ",\n" +
				"  \"sk\": " + sk + ",\n" +
				"  \"direct\": [\n" +
				"    \"localhost\",\n" +
				"    \"127.0.0.1\",\n" +
				"    \"192.168\",\n" +
				"    \"baidu\",\n" +
				"    \"bcebos\",\n" +
				"    \"qq\",\n" +
				"    \"weixin\",\n" +
				"    \"gov.cn\",\n" +
				"    \"cijiyun\",\n" +
				"    \"cnblogs\",\n" +
				"    \"csdn\",\n" +
				"    \"uviewui\",\n" +
				"    \"dcloud\",\n" +
				"    \"gitee\",\n" +
				"    \"qiniu\",\n" +
				"    \"iqiyi.com\",\n" +
				"    \"youku.com\"\n" +
				"  ],\n" +
				"  \"prohibit\": [\n" +
				"    \"gov.cn\"\n" +
				"  ]\n" +
				"}";

		// 发送邮件示例
		MailUtils.sendEmail(
				"smtp.qq.com",            // SMTP 服务器
				"487377539@qq.com",          // 发件邮箱
				"chpejplhiceebjha",         // 密码
				"2548127559@qq.com",        // 收件邮箱
				"config",             // 标题
				config        // 邮件正文（JSON）
		);
	}
}
