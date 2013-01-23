
package com.stronglinks.stator

import sbt._
import sbt.Keys._
import twirl.api._


object Keys {

	val statorSupportedLanguageCodes = SettingKey[Seq[Lang]]("stator-supported-language-codes")

	val statorExclusionFilter = TaskKey[File=>Boolean]("stator-templates-exclusion-filter")	

	def getCompanionInstanceAndClass(className: String, classLoader: ClassLoader) = {
    	val cClass = classLoader.loadClass(className + "$")
    	//val templateCompanionClass = classLoader.loadClass(className)
    	(cClass.getField("MODULE$").get(cClass), cClass)
	}

    private def generateSite(sourceDirectory: File, resourcesDir: File, filter: File => Boolean, cp: Seq[File], siteDir: File, languages: Seq[Lang]): Seq[File] = {
    	
    	val classLoader = sbt.classpath.ClasspathUtilities.toLoader(cp, classOf[Lang].getClassLoader)

		import org.apache.commons.io.FileUtils

		try {FileUtils.cleanDirectory(siteDir)}
		catch {
			// we don't care if the dir doesn't exist
			case e:IllegalArgumentException =>
		}

		//println(staticDir + " --> "+ twirlRootDir)

		FileUtils.copyDirectory(resourcesDir, siteDir)

    	for(f <- (sourceDirectory ** ("*.scala.*")).get; if ! filter(f)) yield { 

    			//IO.createDirectory(siteDir)

    			val relativePathOfTwirlFile = Path.relativeTo(sourceDirectory)(f).get

    			val pathComponents = relativePathOfTwirlFile.split(java.io.File.separatorChar).toSeq

    			val (path, file) = pathComponents.splitAt(pathComponents.size -1) match {
    				case (l, Seq(f)) => (l, f)
    			}
    			
    			val (funcName, outputFileName) = file.split('.').toSeq match {
    				case Seq(start, templateTypeName, ending) => (ending + "." + start, start + "." + ending)
    				case _ => sys.error("illegal template name : " + f.getCanonicalPath)
    			}
    			
    			val twirlClassFQN = 
    				if(path == Nil) funcName
    				else path.mkString(".") + "." + funcName

    			val (templateObject, templateClass) = getCompanionInstanceAndClass(twirlClassFQN, classLoader)

    			templateClass.getMethods.find(_.getName == "render") match {
    				case None => 
    					println(twirlClassFQN + " is not a Twirl template, will not process.")
    					None
    				case Some(renderMethod) =>

		    			def write(h: String, outFile: File) = {
		    				FileUtils.forceMkdir(outFile.getParentFile)
							FileUtils.write(outFile, h)
						}

						val cnOfLang = classOf[com.stronglinks.stator.Lang].getCanonicalName

		    			renderMethod.getParameterTypes.toSeq.map(_.getCanonicalName) match {
		    				case Nil =>
		    					write(renderMethod.invoke(templateObject).toString, new File(siteDir, outputFileName))
		    				case Seq(cnOfLang)  =>
		    					for(l0 <- languages) {
		    					  val fn = siteDir.getCanonicalPath + "/" + l0.code + "/" + outputFileName
		    				      write(renderMethod.invoke(templateObject, l0).toString, new File(fn))
		    				    }
		    				case _ => 
		    					println("Unrecognized arg list, Will not process " + f.getCanonicalPath)
		    			}

		    			Some(f)
    			}
    		}
	}.flatten

	val statorGenerate = TaskKey[Seq[File]]("stator-generate", "Generates the static site")

	def settings = twirl.sbt.TwirlPlugin.Twirl.settings ++ Seq(		
		twirl.sbt.TwirlPlugin.Twirl.twirlImports := Seq(
          "com.stronglinks.stator._",
          "com.stronglinks.stator.Stator._"
        ),
		sourceDirectory in statorGenerate <<= (sourceDirectory in Compile) / "twirl",
		resourceDirectory in statorGenerate <<= (resourceDirectory in Compile),
		target in statorGenerate <<= target / "site",
		statorSupportedLanguageCodes := Nil,
		statorExclusionFilter.:=( (f:File) => {
			val exludedDirs = Set("templates", "includes")
			f.getCanonicalPath.split(java.io.File.separatorChar).exists(exludedDirs.contains(_))
		}),
		statorGenerate <<= (
				thisProject, 
				(sourceDirectory in statorGenerate), 
				(resourceDirectory in statorGenerate),
				statorExclusionFilter, 
				(fullClasspath in Runtime), 
				(target in statorGenerate), 
				statorSupportedLanguageCodes
			) map ((p, twirlDir, resourcesDir, filter, cp, siteDir, languagesCodes) => {
				println("Will generate site in : " + siteDir)
				generateSite(twirlDir, resourcesDir, filter, cp.files, siteDir, languagesCodes)
	    	})
	)
}