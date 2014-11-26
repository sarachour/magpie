package magpie.util;


public class CodeFragment {
	public CodePosition startLoc;
	public CodePosition endLoc;
	public class CodePosition {
		public int linePos;
		public int charPos;
	}
	public CodeFragment(int sl, int sc, int el, int ec){
		startLoc = new CodePosition();
		endLoc = new CodePosition();
		startLoc.linePos = sl;
		startLoc.charPos = sc;
		endLoc.linePos = el;
		endLoc.charPos = ec;
		
	}
}
