package Utils;

public class VerificationResult {

	public int lastActionIndex = -1;
	public boolean isVerified = false;
	public boolean isTimeout = false;

	public VerificationResult(int lastActionIndex, boolean isVerified, boolean isTimeout) {
		this.lastActionIndex = lastActionIndex;
		this.isVerified = isVerified;
		this.isTimeout = isTimeout;
	}

	public VerificationResult(int lastActionIndex, boolean isVerified) {
		this(lastActionIndex,isVerified,false);
	}

}
