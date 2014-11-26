package magpie.util;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.Position;

public class Difference {
	public enum MatchType {
		Same,
		Different,
		Insert,
		Delete
	}
	public Position thisPosition;
	public IResource thisResource;
	public MatchType matchType;
	public Position otherPosition;
	public IResource otherResource;
	public List<String> otherData;
	public Difference(Position src, IResource srcr, Position other, IResource otherr, MatchType isMatch){
		this.thisPosition = src;
		this.thisResource = srcr;
		this.otherPosition = other;
		this.otherResource = otherr;
		this.matchType = isMatch;
	}
	public Difference(Position src, IResource srcr, Position other, IResource otherr, MatchType isMatch, List<String> otherData){
		this.thisPosition = src;
		this.thisResource = srcr;
		this.otherPosition = other;
		this.otherResource = otherr;
		this.matchType = isMatch;
		this.otherData = otherData;
	}
}
