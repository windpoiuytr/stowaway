package org.wind.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
	 * 转字节
	 *
	 * @param msg Netty消息
	 */
	public static byte[] toBytes(Object msg)
	{
		try
		{
			return ByteBufUtil.getBytes((ByteBuf) msg);
		} finally
		{
			ReferenceCountUtil.release(msg); // 释放引用
		}
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
