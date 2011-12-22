package org.flexunit.ant.launcher.commands.player;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.util.ResourceUtils;
import org.flexunit.ant.LoggingUtil;
import org.flexunit.ant.tasks.configuration.ExtensionElement;

public class AdlCommand extends DefaultPlayerCommand
{
   private final String ADT_JAR_PATH = "lib" + File.separatorChar + "adt.jar";
   private final String DESCRIPTOR_TEMPLATE = "flexUnitDescriptor.template";
   private final String DESCRIPTOR_FILE = "flexUnitDescriptor.xml";

   /**
    * The list of native extensions to load.
    */
   private List<ExtensionElement> extensions = null;
   
   /**
    * The path where we look for extensions.
    */
   private List<DirSet> extensionPath = null;
   
   private File precompiledAppDescriptor;
   
   @Override
   public File getFileToExecute()
   {
	  if(getPrecompiledAppDescriptor() != null)
	  {
		  return new File(getPrecompiledAppDescriptor().getAbsolutePath());
	  }
      return new File(getSwf().getParentFile().getAbsolutePath() + File.separatorChar + DESCRIPTOR_FILE);
   }

   /**
    * Used to create the application descriptor used to invoke adl
    */
   private void createApplicationDescriptor()
   {
      try
      {
         //Template location in JAR
         URLResource template = new URLResource(getClass().getResource("/" + DESCRIPTOR_TEMPLATE));

         //Descriptor location, same location as SWF due to relative path required in descriptor
         File descriptor = new File(getSwf().getParentFile().getAbsolutePath() + File.separatorChar + DESCRIPTOR_FILE);

         //Create tokens to filter
         Double versionNumber = getVersion();
         FilterSet filters = new FilterSet();
         filters.addFilter("ADL_SWF", getSwf().getName());
         filters.addFilter("ADT_VERSION", Double.toString(versionNumber));
         if(versionNumber > 2.0) {
        	 filters.addFilter("VERSION_PROP", "versionNumber");
         } else {
        	 filters.addFilter("VERSION_PROP", "version");
         }
         filters.addFilter("EXTENSION_TAGS", getExtensionTags());

         //Copy descriptor template to SWF folder performing token replacement
         ResourceUtils.copyResource(
            template,
            new FileResource(descriptor),
            new FilterSetCollection(filters),
            null,
            true,
            false,
            null,
            null,
            getProject()
         );

         LoggingUtil.log("Created application descriptor at [" + descriptor.getAbsolutePath() + "]");
      }
      catch (Exception e)
      {
         throw new BuildException("Could not create application descriptor");
      }
   }

   /**
    * Generates the required <code>&lt;extensionID&gt;</code> tags in the application
    * descriptor file.
    */
   private String getExtensionTags()
   {
	   if (!this.hasExtensions())
		   return "";
	   
	   StringBuilder sb = new StringBuilder();
	   sb.append("    <extensions>\n");
	   for (ExtensionElement extension: extensions) {
		   sb.append("        <extensionID>" + extension.getName() + "</extensionID>\n");
	   }
	   sb.append("    </extensions>");
	   
	   return sb.toString();
   }
   
   private double getVersion()
   {
      String outputProperty = "AIR_VERSION";

      //Execute mxmlc to find SDK version number
      Java task = new Java();
      task.setFork(true);
      task.setFailonerror(true);
      task.setJar(new File(getProject().getProperty("FLEX_HOME") + File.separatorChar + ADT_JAR_PATH));
      task.setProject(getProject());
      task.setDir(getProject().getBaseDir());
      task.setOutputproperty(outputProperty);

      Argument versionArgument = task.createArg();
      versionArgument.setValue("-version");

      task.execute();
      double version = parseAdtVersionNumber( getProject().getProperty(outputProperty) );
      LoggingUtil.log("Found AIR version: " + version);
      return version;
   }


   private double parseAdtVersionNumber( String versionString )
   {
	  double version;

      //AIR 2.6 and greater only returns the version number.
      if( versionString.startsWith("adt") )
      {
          //Parse version number and return as int
	      int prefixIndex = versionString.indexOf("adt version \"");
	      version = Double.parseDouble(versionString.substring(prefixIndex + 13, prefixIndex + 16));

      }else
      {
    	  version = Double.parseDouble(versionString.substring(0, 3) );
      }

	  return version;
   }

   @Override
   public void prepare()
   {
	  Commandline cmd = this.getCommandLine();
      cmd.setExecutable(generateExecutable());
      
      if(this.hasExtensions())
      {
    	  getCommandLine().addArguments(new String[] { "-profile", "extendedDesktop" });
    	  
    	  if (this.extensionPath != null && this.extensionPath.size() > 0)
    	  {
    		  for (DirSet dirSet: this.extensionPath)
    		  {
    			  cmd.addArguments(new String[] { "-extdir", dirSet.getDir().getAbsolutePath() });
    		  }
    	  }
      }

      cmd.addArguments(new String[]{getFileToExecute().getAbsolutePath()});
      
      if(getPrecompiledAppDescriptor() == null)
      {
    	  //Create Adl descriptor file
    	  createApplicationDescriptor();
      }
   }

   private String generateExecutable()
   {
      return getProject().getProperty("FLEX_HOME") + "/bin/" + getDefaults().getAdlCommand();
   }

   public File getPrecompiledAppDescriptor()
   {
	   return precompiledAppDescriptor;
   }

   public void setPrecompiledAppDescriptor(File precompiledAppDescriptor)
   {
	   this.precompiledAppDescriptor = precompiledAppDescriptor;
   }
   
   public List<ExtensionElement> getExtensions() {
	   if (extensions == null)
		   return null;
	   return Collections.unmodifiableList(extensions);
   }
   
   public boolean hasExtensions() {
	   return extensions != null && !extensions.isEmpty();
   }
   
   public void setExtensions(List<ExtensionElement> extensions) {
	   this.extensions = extensions;
   }
   
   public List<DirSet> getExtensionPath() {
	   return this.extensionPath;
   }
   
   public void setExtensionPath(List<DirSet> path) {
	   this.extensionPath = path;
   }
}