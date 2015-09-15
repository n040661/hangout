package org.ctrip.ops.sysdev.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.hubspot.jinjava.Jinjava;

public class BaseFilter implements Runnable {

	private static final Logger logger = Logger.getLogger(BaseFilter.class
			.getName());

	protected Map config;
	protected List<String> IF;
	protected ArrayBlockingQueue inputQueue;
	protected ArrayBlockingQueue outputQueue;
	protected Jinjava jinjava;

	public BaseFilter(Map config, ArrayBlockingQueue inputQueue) {
		this.config = config;
		this.IF = (List<String>) this.config.get("if");
		this.jinjava = new Jinjava();

		this.inputQueue = inputQueue;

		int queueSize = 1000;
		if (this.config.containsKey("queueSize")) {
			queueSize = (int) this.config.get("queueSize");
		}

		this.outputQueue = new ArrayBlockingQueue(queueSize, false);

		this.prepare();
	}

	protected void prepare() {
	};

	protected void filter(Map event) {
	};

	public void run() {
		while (true) {
			Map event = (Map) this.inputQueue.poll();
			if (event != null) {

				boolean succuess = true;
				if (this.IF != null) {
					for (String c : this.IF) {
						if (this.jinjava.render(c, event = event).equals(
								"false")) {
							succuess = false;
							break;
						}
					}
				}
				if (succuess == false) {
					continue;
				}

				this.filter(event);
				try {
					this.outputQueue.put(event);
				} catch (InterruptedException e) {
					logger.warn("put event to outMQ failed");
					logger.trace(e.getMessage());
				}
			}
		}
	}

	public ArrayBlockingQueue getOutputMQ() {
		return this.outputQueue;
	}

	public static void main(String[] args) {
		Jinjava jinjava = new Jinjava();
		long s = System.currentTimeMillis();
		final Map<String, Object> context = new HashMap();
		context.put("message", "Ja-red");
		context.put("@timestamp", 1442281327000L);

		Map event = new HashMap() {
			{
				put("event", context);
			}
		};

		for (int i = 0; i < 10000; i++) {
			jinjava.render("{{\"-\" in message}}", context);
		}
		System.out.println(System.currentTimeMillis() - s);

		System.out.println(jinjava.render(
				"{{event[\"@timestamp\"]|datetimeformat(\"%Y.%m\")}}",
				event = context));
	}
}
