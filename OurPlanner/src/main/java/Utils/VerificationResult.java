package Utils;

public class VerificationResult {

	public int lastOKActionIndex = -1;
	public boolean isVerified = false;
	public boolean isTimeout = false;

	public VerificationResult(int lastOKActionIndex, boolean isVerified, boolean isTimeout) {
		this.lastOKActionIndex = lastOKActionIndex;
		this.isVerified = isVerified;
		this.isTimeout = isTimeout;
	}

	public VerificationResult(int lastOKActionIndex, boolean isVerified) {
		this(lastOKActionIndex,isVerified,false);
	}

}
