package com.zbennoz.zbenclaims;

public class ClaimResult {
    public enum Type { SUCCESS, ALREADY_CLAIMED, LIMIT_REACHED, NOT_CLAIMED, NOT_OWNER, FAIL }

    private final Type type;
    private final Claim claim;
    private final int currentClaims;
    private final int maxClaims;
    private final String message;

    private ClaimResult(Type type, Claim claim, int currentClaims, int maxClaims, String message) {
        this.type = type;
        this.claim = claim;
        this.currentClaims = currentClaims;
        this.maxClaims = maxClaims;
        this.message = message;
    }

    public static ClaimResult success(Claim claim) { return new ClaimResult(Type.SUCCESS, claim, -1, -1, null); }
    public static ClaimResult alreadyClaimed(Claim claim) { return new ClaimResult(Type.ALREADY_CLAIMED, claim, -1, -1, null); }
    public static ClaimResult limitReached(int current, int limit) { return new ClaimResult(Type.LIMIT_REACHED, null, current, limit, null); }
    public static ClaimResult notClaimed() { return new ClaimResult(Type.NOT_CLAIMED, null, -1, -1, null); }
    public static ClaimResult notOwner(Claim claim) { return new ClaimResult(Type.NOT_OWNER, claim, -1, -1, null); }
    public static ClaimResult fail(String reason) { return new ClaimResult(Type.FAIL, null, -1, -1, reason); }

    public Type type() { return type; }
    public Claim claim() { return claim; }
    public int currentClaims() { return currentClaims; }
    public int maxClaims() { return maxClaims; }
    public String message() { return message; }
}
