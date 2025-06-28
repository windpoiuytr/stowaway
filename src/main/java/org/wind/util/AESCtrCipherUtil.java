package org.wind.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class AESCtrCipherUtil
{
	private final Cipher encryptCipher;
	private final Cipher decryptCipher;

	// 构造函数：传入密钥（16或32字节）和IV（16字节）
	public AESCtrCipherUtil(byte[] sk, byte[] iv) throws GeneralSecurityException
	{
		if (iv.length != 16)
		{
			throw new IllegalArgumentException("IV must be 16 bytes");
		}

		SecretKeySpec keySpec = new SecretKeySpec(sk, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(iv);

		this.encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

		this.decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
	}

	/**
	 * 将 ByteBuf 转换为 byte[]，并确保释放原始 ByteBuf。
	 *
	 * @param msg 输入对象（必须是 ByteBuf 类型）
	 * @return 转换后的 byte[]，如果输入为 null 或非 ByteBuf 类型，返回空数组
	 * @throws IllegalArgumentException 如果输入不是 ByteBuf 类型
	 */
	public static byte[] toBytes(Object msg)
	{
		// 1. 参数校验
		if (msg == null)
		{
			return new byte[0];
		}
		if (!(msg instanceof ByteBuf))
		{
			throw new IllegalArgumentException("Input must be a ByteBuf, but got: " + msg.getClass());
		}

		ByteBuf buf = (ByteBuf) msg;
		// 2. 检查是否可读（避免异常）
		if (!buf.isReadable())
		{
			ReferenceCountUtil.safeRelease(buf); // 安全释放
			return new byte[0];
		}

		// 3. 拷贝数据到 byte[]
		byte[] bytes;
		try
		{
			bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes); // 直接读取，避免 ByteBufUtil 的额外开销
		} catch (Exception e)
		{
			ReferenceCountUtil.safeRelease(buf); // 异常时确保释放
			throw new RuntimeException("Failed to convert ByteBuf to bytes", e);
		}

		// 4. 确保释放（即使 readBytes 抛出异常）
		ReferenceCountUtil.safeRelease(buf);
		return bytes;
	}

	// 加密数据
	public byte[] en(byte[] plainData)
	{
		return encryptCipher.update(plainData);
	}

	// 解密数据
	public byte[] de(byte[] encryptedData)
	{
		return decryptCipher.update(encryptedData);
	}
}
