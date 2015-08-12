package org.unbiquitous.unbihealth.stimdriver;


import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOS;

public class Starter {
	public static void main(String[] args) {
		UOS uos = new UOS();
		InitialProperties initProps = new InitialProperties();
		initProps.addDriver(Stimdriver.class);
		initProps.put(Stimdriver.SERIAL_PORT_PROP_KEY, "/dev/tty.usbserial-HMQYVD6B");
		uos.start(initProps);
	}
}
