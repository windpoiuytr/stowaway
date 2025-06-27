package org.wind.util;

import com.jayway.jsonpath.JsonPath;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.SearchTerm;
import java.io.File;
import java.util.Properties;

public class MailUtils
{

	/**
	 * 发送邮件
	 */
	public static void sendEmail(String smtpHost, String from, String password, String to, String subject, String body) throws Exception
	{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.starttls.enable", "true");

		Session session = Session.getInstance(props, new Authenticator()
		{
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(from, password);
			}
		});

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		message.setText(body); // 纯文本正文

		Transport.send(message);
	}

	/**
	 * 接收最新匹配邮件的正文内容（纯文本）
	 */
	public static String receiveLatestJsonMail(
			String imapHost,
			String username,
			String password,
			String fromFilter,
			String subjectKeyword
	) throws Exception
	{
		Properties props = new Properties();
		props.put("mail.store.protocol", "imaps");

		Session session = Session.getInstance(props);
		Store store = session.getStore();
		store.connect(imapHost, username, password);

		Folder inbox = store.getFolder("INBOX");
		inbox.open(Folder.READ_ONLY);

		SearchTerm subjectTerm = new SearchTerm()
		{
			@Override
			public boolean match(Message message)
			{
				try
				{
					String subject = message.getSubject();
					return subject != null && subject.trim().equalsIgnoreCase(subjectKeyword);
				} catch (MessagingException e)
				{
					return false;
				}
			}
		};

		// 设置筛选条件：发件人 + 主题
		SearchTerm term = new AndTerm(
				new FromStringTerm(fromFilter),
				subjectTerm
		);

		Message[] messages = inbox.search(term);

		if (messages.length == 0) return null;

		// 获取最新一封匹配邮件（按时间）
		Message latest = messages[0];
		for (Message msg : messages)
		{
			if (msg.getSentDate().after(latest.getSentDate()))
			{
				latest = msg;
			}
		}

		String content = getTextFromMessage(latest);

		inbox.close(false);
		store.close();
		return content;
	}

	/**
	 * 提取邮件正文文本
	 */
	private static String getTextFromMessage(Message message) throws Exception
	{
		if (message.isMimeType("text/plain"))
		{
			return message.getContent().toString();
		} else if (message.isMimeType("multipart/*"))
		{
			Multipart mp = (Multipart) message.getContent();
			for (int i = 0; i < mp.getCount(); i++)
			{
				BodyPart part = mp.getBodyPart(i);
				if (part.isMimeType("text/plain"))
				{
					return part.getContent().toString();
				}
			}
		}
		return null;
	}

	public static void main(String[] args) throws Exception
	{
		// 读取配置文件
		String config = JsonPath.parse(new File("config.json")).jsonString();
		// 发送邮件示例
		MailUtils.sendEmail(
				"smtp.qq.com",            // SMTP 服务器
				"487377539@qq.com",          // 发件邮箱
				"chpejplhiceebjha",         // 密码
				"2548127559@qq.com",        // 收件邮箱
				"config",             // 标题
				config        // 邮件正文（JSON）
		);

		// 接收匹配邮件示例
		// String json = MailUtil.receiveLatestJsonMail(
		// 		"imap.qq.com",            // IMAP 服务器
		// 		"2548127559@qq.com",        // 登录邮箱
		// 		"fnookyyazkeaebha",         // 密码
		// 		"487377539@qq.com",          // 过滤：发件人
		// 		"config"              // 过滤：标题关键词
		// );
		//
		// System.out.println("收到配置JSON：\n" + json);
	}
}
