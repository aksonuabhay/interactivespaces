package interactivespaces.service.image.depth.internal.openni2.libraries;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * <i>native declaration : NiteCTypes.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("NiTE2") 
public class NiteUserMap extends StructObject {
	/** C type : NiteUserId* */
	@Field(0) 
	public Pointer<Short > pixels() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : NiteUserId* */
	@Field(0) 
	public NiteUserMap pixels(Pointer<Short > pixels) {
		this.io.setPointerField(this, 0, pixels);
		return this;
	}
	@Field(1) 
	public int width() {
		return this.io.getIntField(this, 1);
	}
	@Field(1) 
	public NiteUserMap width(int width) {
		this.io.setIntField(this, 1, width);
		return this;
	}
	@Field(2) 
	public int height() {
		return this.io.getIntField(this, 2);
	}
	@Field(2) 
	public NiteUserMap height(int height) {
		this.io.setIntField(this, 2, height);
		return this;
	}
	@Field(3) 
	public int stride() {
		return this.io.getIntField(this, 3);
	}
	@Field(3) 
	public NiteUserMap stride(int stride) {
		this.io.setIntField(this, 3, stride);
		return this;
	}
	public NiteUserMap() {
		super();
	}
	public NiteUserMap(Pointer pointer) {
		super(pointer);
	}
}