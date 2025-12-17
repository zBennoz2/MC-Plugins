package com.zbennoz.zbenclaims;

public class ClaimResult {
    public enum Type { SUCCESS, ALREADY_CLAIMED, LIMIT_REACHED, NOT_CLAIMED, NOT_OWNER, FAIL }

    private final Type type;
    private final Claim claim;
    private final int limit;

    private ClaimResult(Type type, Claim claim, int limit) {
        this.type = type;
        this.claim = claim;
        this.limit = limit;
    }

    public static ClaimResult success(Claim claim) { return new ClaimResult(Type.SUCCESS, claim, -1); }
    public static ClaimResult alreadyClaimed(Claim claim) { return new ClaimResult(Type.ALREADY_CLAIMED, claim, -1); }
    public static ClaimResult limitReached(int limit) { return new ClaimResult(Type.LIMIT_REACHED, null, limit); }
    public static ClaimResult notClaimed() { return new ClaimResult(Type.NOT_CLAIMED, null, -1); }
    public static ClaimResult notOwner(Claim claim) { return new ClaimResult(Type.NOT_OWNER, claim, -1); }
    public static ClaimResult fail(String reason) { return new ClaimResult(Type.FAIL, null, -1); }

    public Type type() { return type; }
    public Claim claim() { return claim; }
    public int limit() { return limit; }
}
