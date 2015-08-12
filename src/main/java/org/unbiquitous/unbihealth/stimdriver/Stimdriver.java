package org.unbiquitous.unbihealth.stimdriver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.driverManager.UosDriver;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDriver;

public class Stimdriver implements UosDriver {
	public static final String DRIVER_NAME = "org.unbiquitous.unbihealth.Stimdriver";
	public static final String MOVE_EVENT_KEY = "move";
	public static final String SERIAL_PORT_PROP_KEY = "unbihealth.stimdriver.serialport";

	private static final UpDriver _driver = new UpDriver(DRIVER_NAME);
	{
		_driver.addEvent(MOVE_EVENT_KEY);
	}
	private static Logger logger = UOSLogging.getLogger();

	// private Gateway gateway;
	private InitialProperties initProps;
	// private HashMap<UpNetworkInterface, UpDevice> listeners = new
	// HashMap<UpNetworkInterface, UpDevice>();
	private boolean running = false;
	private SerialPort port;

	public UpDriver getDriver() {
		return _driver;
	}

	public List<UpDriver> getParent() {
		return null;
	}

	public void init(Gateway _gateway, InitialProperties _initProps, String id) {
		// this.gateway = _gateway;
		this.initProps = _initProps;

		new SerialPortThread().start();
	}

	public void destroy() {
		running = false;
	}

	private class SerialPortThread extends Thread {
		// private Vector3 lastData = new Vector3(0, 0, 0);
		private long lastT = System.currentTimeMillis();

		@Override
		public void run() {
			System.out.println("Start");

			// for (int i = 0; i < xBytes.length; i++) {
			// System.out.println(xBytes[i]);
			// }

			// System.out.println(sb.toString());

			try {
				port = new SerialPort(initProps.getString(SERIAL_PORT_PROP_KEY));
				if (port.openPort()) {
					port.setParams(115200, 8, 2, 0);
					// running = true;

					// while (running) {

					initialization(port);

					long t = System.currentTimeMillis();
					System.out.println("Time: " + (t - lastT));
					lastT = t;
					// }
				}
			} catch (SerialPortException e) {
				logger.log(Level.SEVERE, "serial port failure", e);
			}
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
		}

	}

	public String int2bin(double group_time, int lenght) {
		String bin = Integer.toBinaryString((int) group_time);
		if (bin.length() < lenght) {
			int number_of_zeros = lenght - bin.length();
			for (int i = 0; i < number_of_zeros; i++) {
				bin = "0" + bin;
			}
		}
		return bin;
	}

	public byte[] initialization(SerialPort port) {

		long ts1;
		long ts2;
		int n_factor;
		int channels[];
		int channels_lf[];
		double main_time;
		double group_time;
		long checksum;

		ts1 = 50;
		ts2 = 5;
		n_factor = 1;
		channels = new int[] { 1 };
		channels_lf = new int[] { 5 };

		main_time = (ts1 - 1) / 0.5;
		group_time = (ts2 - 1.5) / 0.5;
		int number_channels = channels.length;
		int channel_stimulated = 0;
		int channel_lf_stimulated = 0;
		for (int i = 0; i < number_channels; i++) {
			channel_stimulated = (int) (channel_stimulated + Math.pow(2,
					channels[i] - 1));
		}
		number_channels = channels_lf.length;
		for (int i = 0; i < number_channels; i++) {
			channel_lf_stimulated = (int) (channel_lf_stimulated + Math.pow(2,
					channels_lf[i] - 1));
		}

		checksum = (long) ((n_factor + channel_stimulated
				+ channel_lf_stimulated + main_time + group_time) % 8);
		String checksum_bin = int2bin((int) checksum, 3);
		String n_factor_bin = int2bin(n_factor, 3);
		String channel_stimulated_bin = int2bin(channel_stimulated, 8);
		String channel_lf_stimulated_bin = int2bin(channel_lf_stimulated, 8);
		String group_time_bin = int2bin(group_time, 5);
		String main_time_bin = int2bin(main_time, 11);
		String ident = "100";
		String[] bytes = new String[6];// = new
										// String[]{"1","1","1","1","1","1"};
		bytes[0] = ident + checksum_bin + n_factor_bin.substring(0, 1)
				+ n_factor_bin.substring(1, 2);
		bytes[1] = "0" + n_factor_bin.substring(2, 3)
				+ channel_stimulated_bin.substring(0, 6);
		bytes[2] = "0" + channel_stimulated_bin.substring(6, 8)
				+ channel_lf_stimulated_bin.substring(0, 5);
		bytes[3] = "0" + channel_lf_stimulated_bin.substring(5, 8) + "00"
				+ group_time_bin.substring(0, 2);
		bytes[4] = "0" + group_time_bin.substring(2, 5)
				+ main_time_bin.substring(0, 4);
		bytes[5] = "0" + main_time_bin.substring(4, 11);
		// for (int i = 0; i < bytes.length; i++) {
		// System.out.println(bytes[i]);
		// }

		byte[] xBytes = new byte[6];
		for (int i = 0; i < xBytes.length; i++) {
			xBytes[i] = (byte) Short.parseShort(bytes[i], 2);
		}

		byte[] s = null;
		try {
			port.writeBytes(xBytes);
			Thread.sleep(50);
			s = port.readBytes();
			StringBuilder sb = new StringBuilder();
			for (byte b : s) {
				sb.append(String.format("%02X ", b));
			}
			System.out.print(sb);
		} catch (SerialPortException e) {
			logger.log(Level.SEVERE, "serial port failure", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return s;
	}
	
	public byte[] update(SerialPort port){
		long ts1;
		long ts2;
		int n_factor;
		int channels[];
		int channels_lf[];
		double main_time;
		double group_time;
		long checksum;

		ts1 = 50;
		ts2 = 5;
		n_factor = 1;
		channels = new int[] { 1 };
		channels_lf = new int[] { 5 };

		main_time = (ts1 - 1) / 0.5;
		group_time = (ts2 - 1.5) / 0.5;
		int number_channels = channels.length;
		int channel_stimulated = 0;
		int channel_lf_stimulated = 0;
		for (int i = 0; i < number_channels; i++) {
			channel_stimulated = (int) (channel_stimulated + Math.pow(2,
					channels[i] - 1));
		}
		number_channels = channels_lf.length;
		for (int i = 0; i < number_channels; i++) {
			channel_lf_stimulated = (int) (channel_lf_stimulated + Math.pow(2,
					channels_lf[i] - 1));
		}

		checksum = (long) ((n_factor + channel_stimulated
				+ channel_lf_stimulated + main_time + group_time) % 8);
		String checksum_bin = int2bin((int) checksum, 3);
		String n_factor_bin = int2bin(n_factor, 3);
		String channel_stimulated_bin = int2bin(channel_stimulated, 8);
		String channel_lf_stimulated_bin = int2bin(channel_lf_stimulated, 8);
		String group_time_bin = int2bin(group_time, 5);
		String main_time_bin = int2bin(main_time, 11);
		String ident = "100";
		String[] bytes = new String[6];// = new
										// String[]{"1","1","1","1","1","1"};
		bytes[0] = ident + checksum_bin + n_factor_bin.substring(0, 1)
				+ n_factor_bin.substring(1, 2);
		bytes[1] = "0" + n_factor_bin.substring(2, 3)
				+ channel_stimulated_bin.substring(0, 6);
		bytes[2] = "0" + channel_stimulated_bin.substring(6, 8)
				+ channel_lf_stimulated_bin.substring(0, 5);
		bytes[3] = "0" + channel_lf_stimulated_bin.substring(5, 8) + "00"
				+ group_time_bin.substring(0, 2);
		bytes[4] = "0" + group_time_bin.substring(2, 5)
				+ main_time_bin.substring(0, 4);
		bytes[5] = "0" + main_time_bin.substring(4, 11);
		// for (int i = 0; i < bytes.length; i++) {
		// System.out.println(bytes[i]);
		// }

		byte[] xBytes = new byte[6];
		for (int i = 0; i < xBytes.length; i++) {
			xBytes[i] = (byte) Short.parseShort(bytes[i], 2);
		}

		byte[] s = null;
		try {
			port.writeBytes(xBytes);
			Thread.sleep(50);
			s = port.readBytes();
			StringBuilder sb = new StringBuilder();
			for (byte b : s) {
				sb.append(String.format("%02X ", b));
			}
			System.out.print(sb);
		} catch (SerialPortException e) {
			logger.log(Level.SEVERE, "serial port failure", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return s;
	}
}
