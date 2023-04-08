package pambatch.ctrl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;

import com.sun.management.OperatingSystemMXBean;
import java.util.Set;

import javax.management.MBeanServer;

/**
 * Get some info on CPU usage, memory, etc. 
 * @author dg50
 *
 */
public class PCMonitor {

	public static void main(String[] args) {

		Set<Class<? extends PlatformManagedObject>> managementInterfaces = ManagementFactory.getPlatformManagementInterfaces();
//		for (Class<? extends PlatformManagedObject> managementObject : managementInterfaces) {
//			System.out.println(managementObject.toString());
//		}
		MBeanServer mbsc = ManagementFactory.getPlatformMBeanServer();

		OperatingSystemMXBean osMBean = null;
		try {
			osMBean = ManagementFactory.newPlatformMXBeanProxy(
					mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long nanoBefore = System.nanoTime();
//		osMBean.
		double loadAverage = osMBean.getSystemLoadAverage();
		int nProcessors = osMBean.getAvailableProcessors();
		System.out.printf("Load average on %d processors is %3.3f\n", nProcessors, loadAverage);
		//long cpuBefore = osMBean.getProcessCpuTime();
		//
		//// Call an expensive task, or sleep if you are monitoring a remote process
		//
		//long cpuAfter = osMBean.getProcessCpuTime();
		//long nanoAfter = System.nanoTime();
		//
		//long percent;
		//if (nanoAfter > nanoBefore)
		// percent = ((cpuAfter-cpuBefore)*100L)/
		//   (nanoAfter-nanoBefore);
		//else percent = 0;
		java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		double la = osBean.getSystemLoadAverage();
		System.out.println("la = " + la);
		//		
		double totalLoad = 0;
		int nAverage = 10;
		double[] loadhistory = new double[nAverage];
		for (int i = 0; i < 10000; i++) {
			double cpuLoad = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class).getCpuLoad(); // something sensible. 
//			double cpuLoad2 = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class).getSystemLoadAverage(); // always -1
			loadhistory[i%nAverage] = cpuLoad;
			totalLoad = 0;
			for (int j = 0; j < nAverage; j++) {
			totalLoad += loadhistory[j];
			}
			double meanLoad = totalLoad/Math.min(i+1, nAverage);
			System.out.printf("cpuLoad %d current %3.2f%%, mean %3.2f%%\n", i,cpuLoad*100, meanLoad*100);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		
//		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
//		
//		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
//		threadBean.
//		System.
	}
}
