package com.orientechnologies.orient.core.metadata.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;

/**
 * Created by emrul on 27/10/2014.
 *
 * @author Emrul Islam <emrul@emrul.com> Copyright 2014 Emrul Islam
 */
public interface OTokenHandler {
  // Return null if token is unparseable or fails verification.
  // The returned token should be checked to ensure isVerified == true.
  public OToken parseWebToken(byte tokenBytes[]) throws InvalidKeyException, NoSuchAlgorithmException, IOException;
  public OToken parseBinaryToken(byte tokenBytes[]) throws InvalidKeyException, NoSuchAlgorithmException, IOException;

  public boolean validateToken(OToken token, String command, String database);

  // Return a byte array representing a signed token
  public byte[] getSignedWebToken(ODatabaseDocumentInternal db, OSecurityUser user);
  
  public byte[] getSignedBinaryToken(ODatabaseDocumentInternal db, OSecurityUser user);

}
