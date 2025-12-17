package com.zbennoz.zbenclaims;

public class TrustResult {
    public enum Type { SUCCESS, NOT_CLAIMED, NOT_OWNER, ALREADY_TRUSTED, NOT_TRUSTED, CANNOT_TRUST_OWNER }

    private final Type type;

    private TrustResult(Type type) {
        this.type = type;
    }

    public static TrustResult success() { return new TrustResult(Type.SUCCESS); }
    public static TrustResult notClaimed() { return new TrustResult(Type.NOT_CLAIMED); }
    public static TrustResult notOwner() { return new TrustResult(Type.NOT_OWNER); }
    public static TrustResult alreadyTrusted() { return new TrustResult(Type.ALREADY_TRUSTED); }
    public static TrustResult notTrusted() { return new TrustResult(Type.NOT_TRUSTED); }
    public static TrustResult cannotTrustOwner() { return new TrustResult(Type.CANNOT_TRUST_OWNER); }

    public Type type() { return type; }
}
