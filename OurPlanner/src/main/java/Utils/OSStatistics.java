package Utils;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class OSStatistics {

	public static double GetMemoryUsage() {
		OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

		double freeSpace = (double)osBean.getFreePhysicalMemorySize();
		double totalSpace = (double)osBean.getTotalPhysicalMemorySize();
		
		double freeSpaceRatio = freeSpace/totalSpace;

		
		return 0;
		//return 1 - freeSpaceRatio;

	}

}
