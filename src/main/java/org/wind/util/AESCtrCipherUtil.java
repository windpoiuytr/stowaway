package org.wind.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * AES CTR 模式加解密工具类
 * <p>
 * 使用 AES/CTR/NoPadding，支持一次性加解密数据块。
 * 适合网络数据流场景，避免块填充带来的额外开销。
 */
public class AESCtrCipherUtil
{
	private final Cipher encryptCipher;
	private final Cipher decryptCipher;

	/**
	 * 构造方法
	 *
	 * @param sk 16 或 32 字节的 AES 密钥
	 * @param iv 16 字节的初始向量（IV），必须是 16 字节
	 * @throws GeneralSecurityException 初始化加解密器异常
	 */
	public AESCtrCipherUtil(byte[] sk, byte[] iv) throws GeneralSecurityException
	{
		if (iv.length != 16)
		{
			throw new IllegalArgumentException("IV must be 16 bytes");
		}

		SecretKeySpec keySpec = new SecretKeySpec(sk, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(iv);

		// 初始化加密器
		this.encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

		// 初始化解密器
		this.decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
	}

	/**
	 * 将 Netty 的 ByteBuf 转为 byte[]，并确保释放 ByteBuf 资源，避免内存泄漏。
	 * <p>
	 * 设计要点：
	 * - 只支持 ByteBuf 类型，否则抛异常。
	 * - 避免复制无效数据（非可读区），返回空数组。
	 * - 保证异常时资源释放。
	 *
	 * @param msg 消息对象，必须是 ByteBuf
	 * @return 转换后字节数组，可能为空数组
	 * @throws IllegalArgumentException 如果 msg 不是 ByteBuf 类型
	 */
	public static byte[] toBytes(Object msg)
	{
		if (msg == null)
		{
			return new byte[0];
		}
		if (!(msg instanceof ByteBuf))
		{
			throw new IllegalArgumentException("Input must be a ByteBuf, but got: " + msg.getClass());
		}

		ByteBuf buf = (ByteBuf) msg;
		if (!buf.isReadable())
		{
			// 不可读时释放资源，避免内存泄漏
			ReferenceCountUtil.safeRelease(buf);
			return new byte[0];
		}

		byte[] bytes = new byte[buf.readableBytes()];
		try
		{
			buf.readBytes(bytes);
		} catch (Exception e)
		{
			ReferenceCountUtil.safeRelease(buf);
			throw new RuntimeException("Failed to convert ByteBuf to bytes", e);
		}

		ReferenceCountUtil.safeRelease(buf);
		return bytes;
	}

	/**
	 * 加密数据
	 * <p>
	 * 注意：
	 * - 仅支持单次 update 加密，不支持 doFinal，适合流式加密。
	 * - 输入数据为空时返回空数组，避免空指针。
	 *
	 * @param plainData 明文字节数组
	 * @return 密文字节数组
	 */
	public byte[] en(byte[] plainData)
	{
		if (plainData == null || plainData.length == 0)
		{
			return new byte[0];
		}
		return encryptCipher.update(plainData);
	}

	/**
	 * 解密数据
	 * <p>
	 * 注意：
	 * - 仅支持单次 update 解密，不支持 doFinal，适合流式解密。
	 * - 输入数据为空时返回空数组，避免空指针。
	 *
	 * @param encryptedData 密文字节数组
	 * @return 明文字节数组
	 */
	public byte[] de(byte[] encryptedData)
	{
		if (encryptedData == null || encryptedData.length == 0)
		{
			return new byte[0];
		}
		return decryptCipher.update(encryptedData);
	}
}
