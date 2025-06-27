package org.wind.util;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class Config
{
	// 配置文件
	public static DocumentContext data;

	public static void get() throws Exception
	{
		// 模拟
		// String json = "{\n" +
		// 		"  \"host\": \"127.0.0.1\",\n" +
		// 		"  \"port\": 41598,\n" +
		// 		"  \"sk\": \"poi951kiu236kjh9\",\n" +
		// 		"  \"direct\": [\n" +
		// 		"    \"localhost\",\n" +
		// 		"    \"127.0.0.1\",\n" +
		// 		"    \"192.168\",\n" +
		// 		"    \"baidu\",\n" +
		// 		"    \"bcebos\",\n" +
		// 		"    \"qq\",\n" +
		// 		"    \"weixin\",\n" +
		// 		"    \"gov.cn\",\n" +
		// 		"    \"cijiyun\",\n" +
		// 		"    \"cnblogs\",\n" +
		// 		"    \"csdn\",\n" +
		// 		"    \"uviewui\",\n" +
		// 		"    \"dcloud\",\n" +
		// 		"    \"gitee\",\n" +
		// 		"    \"qiniu\",\n" +
		// 		"    \"iqiyi.com\",\n" +
		// 		"    \"youku.com\"\n" +
		// 		"  ],\n" +
		// 		"  \"prohibit\": [\n" +
		// 		"    \"qwertyuiop\"\n" +
		// 		"  ]\n" +
		// 		"}";
		// data = JsonPath.parse(json);
		// System.out.println(data.jsonString());


		// 获取邮箱并解析配置文件
		String json = MailUtils.receiveLatestJsonMail(
				"imap.qq.com",            // IMAP 服务器
				"2548127559@qq.com",        // 登录邮箱
				"fnookyyazkeaebha",         // 密码
				"487377539@qq.com",          // 过滤：发件人
				"config"              // 过滤：标题关键词
		);
		data = JsonPath.parse(json);
		System.out.println(data.jsonString());
	}
}
