package com.loit.apps.project.palace.socket.protocal;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class EquProtocalDecoder implements ProtocolDecoder {
	private final AttributeKey CONTEXT = new AttributeKey(getClass(), "context");
	private final Charset charset;

	public EquProtocalDecoder() {
		this(Charset.defaultCharset());
	}

	public EquProtocalDecoder(Charset charset) {
		this.charset = charset;
	}

	private Context getContext(IoSession session) {
		Context ctx = (Context) session.getAttribute(CONTEXT);
		if (ctx == null) {
			ctx = new Context();
			session.setAttribute(CONTEXT, ctx);
		}
		return ctx;
	}

	/**
	 * 解码协议
	 */
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		final int packHeadLength = 3;
		// 先获取上次的处理上下文，其中可能有未处理完的数据
		Context ctx = getContext(session);
		// 先把当前buffer中的数据追加到Context的buffer当中
		ctx.append(in);
		// 把position指向0位置，把limit指向原来的position位置
		IoBuffer buf = ctx.getBuffer();
		buf.flip();
		// 然后按数据包的协议进行读取
		while (buf.remaining() >= packHeadLength) {
			buf.mark();
			// 读取消息头部分
			byte starter = buf.get();
			short length = buf.getShort();
			// 检查读取的包头是否正常，不正常的话清空buffer，丢弃数据
			if (starter != EquProtocalPack.STARTER || length < 0) {
				System.out.println("协议包头不正确，或者长度错误，丢弃数据，请重发！");
				buf.position(buf.limit());
				break;
			} else if (length <= buf.remaining() - 1) {// 读取正常的消息包，并写入输出流中，以便IoHandler进行处理
				int oldLimit = buf.limit();
				buf.limit(buf.position() + length + 1);
				byte[] content = new byte[length];
				buf.get(content);
				byte sum = buf.get();
				buf.limit(oldLimit);
				EquProtocalPack pack = new EquProtocalPack(content, sum);
				out.write(pack);
			} else {// 如果消息包不完整，将指针重新移动消息头的起始位置
				buf.reset();
				break;
			}
		}

		if (buf.hasRemaining()) {// 将数据移到buffer的最前面
			IoBuffer temp = IoBuffer.allocate(100).setAutoExpand(true);
			temp.put(buf);
			temp.flip();
			buf.clear();
			buf.put(temp);
		} else {// 如果数据已经处理完毕，进行清空
			buf.clear();
		}
	}

	public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
	}

	public void dispose(IoSession session) throws Exception {
		Context ctx = (Context) session.getAttribute(CONTEXT);
		if (ctx != null) {
			session.removeAttribute(CONTEXT);
		}
	}

	/**
	 * 记录上下文，因为数据触发没有规模，很可能只收到数据包的一半，所以，需要上下文拼起来才能完整的处理
	 * 
	 * @author sunsw
	 *
	 */
	private class Context {
		private final CharsetDecoder decoder;
		private IoBuffer buf;
		private int matchCount = 0;
		private int overflowPosition = 0;

		private Context() {
			this.decoder = charset.newDecoder();
			this.buf = IoBuffer.allocate(100).setAutoExpand(true);
		}

		public CharsetDecoder getDecoder() {
			return this.decoder;
		}

		public IoBuffer getBuffer() {
			return this.buf;
		}

		public int getOverflowPosition() {
			return this.overflowPosition;
		}

		public int getMatchCount() {
			return this.matchCount;
		}

		public void setMatchCount(int matchCount) {
			this.matchCount = matchCount;
		}

		public void reset() {
			this.overflowPosition = 0;
			this.matchCount = 0;
			this.decoder.reset();
		}

		public void append(IoBuffer in) {
			this.buf.put(in);
		}
	}

}