package org.eclipse.jkube.kit.common;

import java.net.URL;
import java.util.Properties;

public class PropertiesExtender extends Properties {
  private URL propertiesFile;
  
  public void setPropertiesFile(URL file) {
    this.propertiesFile = file;
  }
  
  public URL getPropertiesFile() {
    return this.propertiesFile;
  }
}
