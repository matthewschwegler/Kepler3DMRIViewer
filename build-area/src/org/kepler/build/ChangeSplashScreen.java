/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author$'
 * '$Date$'
 * '$Revision$'
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */
package org.kepler.build;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.PropertyDefaults;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Created by David Welker.
 * Date: Nov 23, 2010
 * Time: 9:17:14 PM
 */
public class ChangeSplashScreen extends ModulesTask
{
    private String version;

    public void setVersion(String version)
    {
        this.version = version;
    }

    @Override
    public void run() throws Exception
    {
        if (version.equals(PropertyDefaults.getDefaultValue("version")))
        {
            PrintError.message("You must specify a version (i.e. -Dversion=1.0)");
            return;
        }

        Module commonModule = null;
        for (Module module : moduleTree)
        {
            if (module.getStemName().equals("common"))
            {
                commonModule = module;
                break;
            }
        }
        if (commonModule == null)
        {
            PrintError.message("The current modules.txt must contain a version of the \"common\" module to change the splash screen.");
            return;
        }
        System.out.println("Changing the splash screen in " + commonModule + "...");
        changeKeplerSplashPng(commonModule, version);
        changeKeplerAboutPng(commonModule, version);
        changeIntroHtml(commonModule, version);
    }

    private void changeKeplerSplashPng(Module commonModule, String version) throws IOException
    {
        BufferedImage templateImg = ImageIO.read(ProjectLocator.getBuildResourceFile("splash-screen", "kepler-splash-template.png"));
        File splashImgFile = new File(commonModule.getResourcesDir(), "images/kepler-splash.png");
        Graphics2D g = templateImg.createGraphics();
        g.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.PLAIN, 14);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        g.drawString("Version " + version, 244, 170);
        ImageIO.write(templateImg, "png", splashImgFile);
    }

    private void changeKeplerAboutPng(Module commonModule, String version) throws IOException
    {
        BufferedImage templateImg = ImageIO.read(ProjectLocator.getBuildResourceFile("splash-screen", "kepler-about-template.png"));
        File aboutImgFile = new File(commonModule.getResourcesDir(), "images/kepler-about.png");
        Graphics2D g = templateImg.createGraphics();
        g.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.PLAIN, 14);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        g.drawString("Version " + version, 243, 80);
        g.drawString("http://kepler-project.org", 243, 100);
        ImageIO.write(templateImg, "png", aboutImgFile);
    }

    private void changeIntroHtml(Module commonModule, String version) throws IOException, TemplateException
    {
        File introHtml = new File(commonModule.getDir(), "configs/ptolemy/configs/kepler/intro.htm");

        Configuration cfg = new Configuration();
        //File templatesDir = new File(ProjectLocator.getBuildDir(), "resources/templates");
        cfg.setDirectoryForTemplateLoading(ProjectLocator.getBuildResourcesDir("splash-screen"));
        Template template = cfg.getTemplate("intro.ftl");

        Map root = new HashMap();
        root.put("version", version);

        Writer writer = new FileWriter(introHtml);
        template.process(root, writer);
    }
}
