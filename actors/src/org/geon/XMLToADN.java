/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.geon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.net.ftp.FTP;
import org.dlese.adn.ADNmetadataType;
import org.dlese.adn.ADType;
import org.dlese.adn.BodyType;
import org.dlese.adn.BoundBoxType;
import org.dlese.adn.CatalogEntriesType;
import org.dlese.adn.CatalogType;
import org.dlese.adn.ContributorLifecycleType;
import org.dlese.adn.ContributorsLifecycleType;
import org.dlese.adn.CoordinateSystemType;
import org.dlese.adn.DateInfoType;
import org.dlese.adn.GeneralType;
import org.dlese.adn.GeospatialCoverageType;
import org.dlese.adn.GeospatialCoveragesType;
import org.dlese.adn.KeywordType;
import org.dlese.adn.KeywordsType;
import org.dlese.adn.LifecycleType;
import org.dlese.adn.MetaMetadataType;
import org.dlese.adn.ObjectFactory;
import org.dlese.adn.ObjectsInSpaceType;
import org.dlese.adn.OnlineType;
import org.dlese.adn.OrganizationType;
import org.dlese.adn.PeriodType;
import org.dlese.adn.PeriodsType;
import org.dlese.adn.PersonType;
import org.dlese.adn.ProjectionType;
import org.dlese.adn.RelationsType;
import org.dlese.adn.RelativeType;
import org.dlese.adn.RequirementType;
import org.dlese.adn.RequirementsType;
import org.dlese.adn.RightsType;
import org.dlese.adn.StatusOfType;
import org.dlese.adn.SubjectsType;
import org.dlese.adn.TechnicalType;
import org.dlese.adn.TemporalCoveragesType;
import org.dlese.adn.TermsOfUseType;
import org.dlese.adn.TimeADType;
import org.dlese.adn.TimeAndPeriodType;
import org.dlese.adn.TimeBCType;
import org.dlese.adn.TimeInfoType;
import org.dlese.adn.TimeRelativeType;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.leeds.ccg.shapefile.Shapefile;
import uk.ac.leeds.ccg.shapefile.ShapefileException;
import util.UUIDGen;

//////////////////////////////////////////////////////////////////////////
////XMLToADN
/**
 * This actor converts XML name-value pairs to ADN schema
 * 
 * @author Efrat Jaeger
 * @version $Id: XMLToADN.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class XMLToADN extends TypedAtomicActor {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public XMLToADN(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		datasetURL = new TypedIOPort(this, "datasetURL", true, false);
		datasetURL.setTypeEquals(BaseType.STRING);

		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.STRING);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n"
				+ "<polygon points=\"-15,-15 15,15 15,-15 -15,15\" "
				+ "style=\"fill:white\"/>\n" + "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // public variables ////

	/** Dataset URL. */
	public TypedIOPort datasetURL;

	/** XML name-value input string. */
	public TypedIOPort input;

	/** ADN schema string. */
	public TypedIOPort output;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Consume an XML string representing name value pairs and converts it to an
	 * ADN schema.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director. FIXME: Either verify that it does
	 *                check for the director, or remove this statement. This
	 *                statement occurs in other conversion actor(s) as well.
	 */
	public void fire() throws IllegalActionException {
		shpfis = null;
		String xmlInput = ((StringToken) input.get(0)).stringValue();
		shpURL = ((StringToken) datasetURL.get(0)).stringValue();

		// Note: Following line may assume 1 byte per character, not sure.
		String outputValue = process(xmlInput);
		String adnContent = "";
		try {
			File adn = new File(outputValue);
			Reader in = new FileReader(adn);
			BufferedReader br = new BufferedReader(in);
			String line;
			while ((line = br.readLine()) != null) {
				adnContent += line + "\n";
			}
			br.close();
			in = null;
			// adn.delete();
			_deleteFiles();
		} catch (Exception ex) {
			_debug(ex.getMessage());
			_deleteFiles();
		}
		output.send(0, new StringToken(adnContent));
	}

	private String process(String xmlString) {

		Map metadata = new HashMap();

		// Extracting user input (name/value pairs) into a hashmap.
		int nameStartInd = xmlString.toLowerCase().indexOf("<name>");
		while (nameStartInd != -1) {
			int nameEndInd = xmlString.toLowerCase().indexOf("</name>");
			String name = xmlString.substring(nameStartInd + 6, nameEndInd);
			xmlString = xmlString.substring(nameEndInd + 7);
			int valueStartInd = xmlString.toLowerCase().indexOf("<value>");
			int valueEndInd = xmlString.toLowerCase().indexOf("</value>");
			String value = xmlString.substring(valueStartInd + 7, valueEndInd);
			xmlString = xmlString.substring(valueEndInd + 8);
			metadata.put(name, value);
			nameStartInd = xmlString.toLowerCase().indexOf("<name>");
		}

		String errMsg = "";

		String title = (String) metadata.get("title");
		String subjectss = (String) metadata.get("subjects");
		String keywords = (String) metadata.get("keywords");
		String permission = (String) metadata.get("permission");
		String description = (String) metadata.get("description");

		String personOrOrg1 = (String) metadata.get("person1");
		String role1 = (String) metadata.get("role1");
		String nametitle1 = (String) metadata.get("nametitle1");
		String firstname1 = (String) metadata.get("firstname1");
		String middlename1 = (String) metadata.get("middlename1");
		String lastname1 = (String) metadata.get("lastname1");
		String org1 = (String) metadata.get("org1");

		String email1 = (String) metadata.get("email1");
		String homepage1 = (String) metadata.get("homepage1");

		// handle spatial coverage
		// String min_altitude = (String) metadata.get("min_altitude");
		// String max_altitude = (String) metadata.get("max_altitude");

		String hrizontal = (String) metadata.get("hrizontal");
		String projection = (String) metadata.get("projection");
		String coordinate = (String) metadata.get("coordinate");

		// handle temporal coverage
		String time = (String) metadata.get("time");

		String geologic_time = (String) metadata.get("geologic_time");
		String begin_age = (String) metadata.get("begin_age");
		String end_age = (String) metadata.get("end_age");

		// handle present coverage
		String begin_date = (String) metadata.get("begindate");
		String end_date = (String) metadata.get("enddate");

		String t = time.trim();

		StringTokenizer stb = !time.equals("present") ? null
				: new StringTokenizer(begin_date, "/");

		int bm = !time.equals("present") ? 0 : Integer
				.parseInt(stb.nextToken());
		int bd = !time.equals("present") ? 0 : Integer
				.parseInt(stb.nextToken());
		int by = !time.equals("present") ? 0 : Integer
				.parseInt(stb.nextToken());

		StringTokenizer ste = !t.equals("present") ? null
				: new StringTokenizer(end_date, "/");

		int em;
		if (!t.equals("present")) {
			em = 0;
		} else {
			em = Integer.parseInt(ste.nextToken());
		}

		int ed;
		if (!t.equals("present")) {
			ed = 0;
		} else {
			ed = Integer.parseInt(ste.nextToken());
		}

		int ey;
		if (!t.equals("present")) {
			ey = 0;
		} else {
			ey = Integer.parseInt(ste.nextToken());
		}

		String begin_hour = (String) metadata.get("begin_hour");
		String end_hour = (String) metadata.get("end_hour");
		String begin_min = (String) metadata.get("begin_min");
		String end_min = (String) metadata.get("end_min");
		String begin_sec = (String) metadata.get("begin_sec");
		String end_sec = (String) metadata.get("end_sec");

		int bHour = Integer.parseInt(begin_hour);
		int bMin = Integer.parseInt(begin_min);
		int bSec = Integer.parseInt(begin_sec);

		int eHour = Integer.parseInt(end_hour);
		int eMin = Integer.parseInt(end_min);
		int eSec = Integer.parseInt(end_sec);

		String begin_month;
		String begin_day;
		String begin_year;

		String end_month;
		String end_day;
		String end_year;

		boolean earlier = true;

		if ((by < ey)
				|| (by == ey && bm < em)
				|| (by == ey && bm == em && bd < ed)
				|| (by == ey && bm == em && bd == ed && bHour < eHour)
				|| (by == ey && bm == em && bd == ed && bHour == eHour && bMin < eMin)
				|| (by == ey && bm == em && bd == ed && bHour == eHour
						&& bMin == eMin && bSec <= eSec)) {

			// begin date and time is earlier
			begin_month = Integer.toString(bm);
			begin_day = Integer.toString(bd);
			begin_year = Integer.toString(by);

			end_month = Integer.toString(em);
			end_day = Integer.toString(ed);
			end_year = Integer.toString(ey);

		} else {

			earlier = false;

			begin_month = Integer.toString(em);
			begin_day = Integer.toString(ed);
			begin_year = Integer.toString(ey);

			end_month = Integer.toString(bm);
			end_day = Integer.toString(bd);
			end_year = Integer.toString(by);

			String tmp = begin_hour;
			begin_hour = end_hour;
			end_hour = tmp;

			tmp = begin_min;
			begin_min = end_min;
			end_min = tmp;

			tmp = begin_sec;
			begin_sec = end_sec;
			end_sec = tmp;

		}

		String time_choice;
		if (by >= 0 && ey >= 0) {
			time_choice = "AD";
		} else if (by < 0 && ey < 0) {
			time_choice = "BC";
		} else {
			time_choice = "direct";
		}

		String bc_begin_year = earlier ? Integer.toString(-by) : Integer
				.toString(-ey);
		String bc_end_year = earlier ? Integer.toString(-ey) : Integer
				.toString(-by);

		String d_begin_year = earlier ? Integer.toString(-by) : Integer
				.toString(-ey);
		String d_end_year = earlier ? Integer.toString(ey) : Integer
				.toString(by);

		try {
			ObjectFactory factory = new ObjectFactory();

			ADNmetadataType itemRecord = (ADNmetadataType) factory
					.newInstance(Class.forName("org.dlese.adn.ItemRecord"));

			// //////////////////////////////////////////////////////////
			// //
			// // general
			// //
			// /////////////////////////////////////////////////////////
			GeneralType general = (GeneralType) factory.newInstance(Class
					.forName("org.dlese.adn.GeneralType"));
			general.setTitle(title);
			general.setDescription(description);
			general.setLanguage("en");

			// subjects
			SubjectsType subjects = (SubjectsType) factory.newInstance(Class
					.forName("org.dlese.adn.SubjectsType"));
			general.setSubjects(subjects);
			subjects.getSubject().add(subjectss);

			// keywords
			if (keywords != null) {
				KeywordsType keywordsType = (KeywordsType) factory
						.newInstance(Class
								.forName("org.dlese.adn.KeywordsType"));

				general.setKeywords(keywordsType);
				StringTokenizer st = new StringTokenizer(keywords, ",");
				while (st.hasMoreTokens()) {
					String tmp = st.nextToken().trim();
					KeywordType keyword = (KeywordType) factory
							.newInstance(Class
									.forName("org.dlese.adn.KeywordType"));
					keyword.setValue(tmp);
					keywordsType.getKeyword().add(keyword);
				}
			}
			// lifecycle
			LifecycleType lifecycle = (LifecycleType) factory.newInstance(Class
					.forName("org.dlese.adn.LifecycleType"));

			// set the first contributor
			ContributorsLifecycleType contributors = (ContributorsLifecycleType) factory
					.newInstance(Class
							.forName("org.dlese.adn.ContributorsLifecycleType"));
			lifecycle.setContributors(contributors);

			ContributorLifecycleType author = (ContributorLifecycleType) factory
					.newInstance(Class
							.forName("org.dlese.adn.ContributorLifecycleType"));
			author.setRole(role1);

			if (personOrOrg1.equals("Person")) {
				PersonType person = (PersonType) factory.newInstance(Class
						.forName("org.dlese.adn.PersonType"));
				person.setNameTitle(nametitle1);
				person.setNameFirst(firstname1);
				person.setNameMiddle(middlename1);
				person.setNameLast(lastname1);
				person.setInstName(org1);
				person.setEmailPrimary(email1);
				author.setPerson(person);

				contributors.getContributor().add(author);
			} else {
				OrganizationType org = (OrganizationType) factory
						.newInstance(Class
								.forName("org.dlese.adn.OrganizationType"));
				org.setInstName(org1);
				contributors.getContributor().add(org);
			}

			// //////////////////////////////////////////////////////////
			// //
			// // metametadata
			// //
			// /////////////////////////////////////////////////////////
			MetaMetadataType metaMetadata = (MetaMetadataType) factory
					.newInstance(Class
							.forName("org.dlese.adn.MetaMetadataType"));
			CatalogEntriesType catalogEntries = (CatalogEntriesType) factory
					.newInstance(Class
							.forName("org.dlese.adn.CatalogEntriesType"));

			CatalogType catalog = (CatalogType) factory.newInstance(Class
					.forName("org.dlese.adn.CatalogType"));
			catalog.setValue("shapefile");

			// get unique id
			// UUIDGenerator ug = UUIDGenerator.getInstance();
			// UUID uuid = ug.generateTimeBasedUUID();

			UUIDGen uuidgen = new UUIDGen();
			String uuid = uuidgen.generateUUID();

			catalog.setEntry("GEON-" + uuid);
			catalogEntries.getCatalog().add(catalog);
			metaMetadata.setCatalogEntries(catalogEntries);

			DateInfoType dateInfo = (DateInfoType) factory.newInstance(Class
					.forName("org.dlese.adn.DateInfoType"));
			Calendar now = Calendar.getInstance();
			// dateInfo.setCreated(now.get(Calendar.YEAR)+"-"+now.get(Calendar.MONTH)+"-"+now.get(Calendar.DAY_OF_MONTH));
			dateInfo.setCreated(now);
			dateInfo.setValue("Registered");
			metaMetadata.setDateInfo(dateInfo);

			StatusOfType statusOf = (StatusOfType) factory.newInstance(Class
					.forName("org.dlese.adn.StatusOfType"));
			statusOf.setStatus("Submitted");
			statusOf.setValue("Submitted");
			metaMetadata.setStatusOf(statusOf);

			metaMetadata.setLanguage("en");
			metaMetadata.setCopyright("No");
			metaMetadata.setScheme("No scheme");

			TermsOfUseType termsOfUse = (TermsOfUseType) factory
					.newInstance(Class.forName("org.dlese.adn.TermsOfUseType"));
			termsOfUse.setValue("Terms of use consistent with GEON policy.");
			metaMetadata.setTermsOfUse(termsOfUse);

			// //////////////////////////////////////////////////////////
			// //
			// // technical
			// //
			// /////////////////////////////////////////////////////////
			TechnicalType technical = (TechnicalType) factory.newInstance(Class
					.forName("org.dlese.adn.TechnicalType"));
			OnlineType online = (OnlineType) factory.newInstance(Class
					.forName("org.dlese.adn.OnlineType"));
			online.setPrimaryURL("http://www.geongrid.org");

			RequirementsType requirements = (RequirementsType) factory
					.newInstance(Class
							.forName("org.dlese.adn.RequirementsType"));
			online.setRequirements(requirements);

			RequirementType requirement = (RequirementType) factory
					.newInstance(Class.forName("org.dlese.adn.RequirementType"));
			requirement
					.setReqType("DLESE:General:No specific technical requirements");
			requirements.getRequirement().add(requirement);

			technical.setOnline(online);

			// //////////////////////////////////////////////////////////
			// //
			// // right
			// //
			// /////////////////////////////////////////////////////////
			RightsType rights = (RightsType) factory.newInstance(Class
					.forName("org.dlese.adn.RightsType"));

			rights.setDescription(permission);
			rights.setCost("DLESE:No");

			// //////////////////////////////////////////////////////////
			// //
			// // relation
			// //
			// /////////////////////////////////////////////////////////
			RelationsType relations = (RelationsType) factory.newInstance(Class
					.forName("org.dlese.adn.RelationsType"));

			// //////////////////////////////////////////////////////////
			// //
			// // spatial coverage
			// //
			// /////////////////////////////////////////////////////////
			GeospatialCoveragesType geospatialCoverages = (GeospatialCoveragesType) factory
					.newInstance(Class
							.forName("org.dlese.adn.GeospatialCoveragesType"));

			GeospatialCoverageType geospatialCoverage = (GeospatialCoverageType) factory
					.newInstance(Class
							.forName("org.dlese.adn.GeospatialCoverageType"));

			BodyType body = (BodyType) factory.newInstance(Class
					.forName("org.dlese.adn.BodyType"));
			body.setPlanet("Earth");
			geospatialCoverage.setBody(body);

			geospatialCoverage.setGeodeticDatumGlobalOrHorz(hrizontal);

			ProjectionType proj = (ProjectionType) factory.newInstance(Class
					.forName("org.dlese.adn.ProjectionType"));
			proj.setType(projection);
			proj.setValue("Some projections here");
			geospatialCoverage.setProjection(proj);

			CoordinateSystemType coord = (CoordinateSystemType) factory
					.newInstance(Class
							.forName("org.dlese.adn.CoordinateSystemType"));
			coord.setType(coordinate);
			coord.setValue("Some cordinate system here");
			geospatialCoverage.setCoordinateSystem(coord);

			BoundBoxType box = (BoundBoxType) factory.newInstance(Class
					.forName("org.dlese.adn.BoundBoxType"));
			box.setBbSrcName("Cataloger supplied");

			/*
			 * VertType vert =
			 * (VertType)factory.newInstance(Class.forName("org.dlese.adn.VertType"
			 * )); VertMinMaxType min =
			 * (VertMinMaxType)factory.newInstance(Class
			 * .forName("org.dlese.adn.VertMinMaxType"));
			 * min.setUnits("centimeters (cm)"); min.setValue(new
			 * BigDecimal(min_altitude[0]));
			 * 
			 * VertMinMaxType max =
			 * (VertMinMaxType)factory.newInstance(Class.forName
			 * ("org.dlese.adn.VertMinMaxType"));
			 * max.setUnits("centimeters (cm)"); max.setValue(new
			 * BigDecimal(max_altitude[0]));
			 * 
			 * vert.setVertMin(min); vert.setVertMax(max);
			 * vert.setGeodeticDatumGlobalOrVert("DLESE:CGD28-CDN");
			 * vert.setVertBase("Datum level");
			 * 
			 * box.setBbVert(vert);
			 */

			geospatialCoverage.setBoundBox(box);
			// geospatialCoverage.setDetGeos();

			geospatialCoverages.getGeospatialCoverage().add(geospatialCoverage);

			// //////////////////////////////////////////////////////////
			// //
			// // temporal coverage
			// //
			// /////////////////////////////////////////////////////////
			TemporalCoveragesType temporalCoverages = (TemporalCoveragesType) factory
					.newInstance(Class
							.forName("org.dlese.adn.TemporalCoveragesType"));
			TimeAndPeriodType timeAndPeriod = (TimeAndPeriodType) factory
					.newInstance(Class
							.forName("org.dlese.adn.TimeAndPeriodType"));
			temporalCoverages.getTimeAndPeriod().add(timeAndPeriod);

			// set time directly into relativeTime
			TimeInfoType timeInfo = (TimeInfoType) factory.newInstance(Class
					.forName("org.dlese.adn.TimeInfoType"));

			timeAndPeriod.setTimeInfo(timeInfo);

			if (time.equals("notpresent")) {
				if (geologic_time.equals("other")) {

					TimeRelativeType timeRelative = (TimeRelativeType) factory
							.newInstance(Class
									.forName("org.dlese.adn.TimeRelativeType"));
					timeInfo.setTimeRelative(timeRelative);

					RelativeType begin = (RelativeType) factory
							.newInstance(Class
									.forName("org.dlese.adn.RelativeType"));
					timeRelative.setBegin(begin);
					begin.setValue(new BigDecimal(begin_age));
					begin.setUnits("ma");

					RelativeType end = (RelativeType) factory.newInstance(Class
							.forName("org.dlese.adn.RelativeType"));
					timeRelative.setEnd(end);
					end.setValue(new BigDecimal(end_age));
					end.setUnits("ma");

				} else {

					TimeRelativeType timeRelative = (TimeRelativeType) factory
							.newInstance(Class
									.forName("org.dlese.adn.TimeRelativeType"));
					timeInfo.setTimeRelative(timeRelative);

					RelativeType begin = (RelativeType) factory
							.newInstance(Class
									.forName("org.dlese.adn.RelativeType"));
					timeRelative.setBegin(begin);
					begin.setValue(new BigDecimal(0));
					begin.setUnits("ma");

					RelativeType end = (RelativeType) factory.newInstance(Class
							.forName("org.dlese.adn.RelativeType"));
					timeRelative.setEnd(end);
					end.setValue(new BigDecimal(0));
					end.setUnits("ma");

					// set time to periods
					PeriodsType periods = (PeriodsType) factory
							.newInstance(Class
									.forName("org.dlese.adn.PeriodsType"));
					timeAndPeriod.setPeriods(periods);

					PeriodType period = (PeriodType) factory.newInstance(Class
							.forName("org.dlese.adn.PeriodType"));
					periods.getPeriod().add(period);
					period.setName(geologic_time);
					period.setSource("USGS-Geologic-Time-Scale");

				}
			} else if (time.equals("present")) {

				// set time directly into timeAD or timeBC
				if (time_choice.equals("AD")) {

					TimeADType timeAD = (TimeADType) factory.newInstance(Class
							.forName("org.dlese.adn.TimeADType"));
					timeInfo.setTimeAD(timeAD);

					Calendar begin = Calendar.getInstance();
					begin.clear();

					begin.add(Calendar.YEAR,
							Integer.parseInt(begin_year) - 1970);
					begin
							.add(Calendar.MONTH,
									Integer.parseInt(begin_month) - 1);
					begin.add(Calendar.DAY_OF_MONTH, Integer
							.parseInt(begin_day) - 1);

					Calendar bt = Calendar.getInstance();
					bt.clear();

					bt.add(Calendar.HOUR, Integer.parseInt(begin_hour));
					bt.add(Calendar.MINUTE, Integer.parseInt(begin_min));
					bt.add(Calendar.SECOND, Integer.parseInt(begin_sec));

					Calendar end = Calendar.getInstance();
					end.clear();
					end.add(Calendar.YEAR, Integer.parseInt(end_year) - 1970);
					end.add(Calendar.MONTH, Integer.parseInt(end_month) - 1);
					end.add(Calendar.DAY_OF_MONTH,
							Integer.parseInt(end_day) - 1);

					Calendar et = Calendar.getInstance();
					et.clear();

					et.add(Calendar.HOUR, Integer.parseInt(end_hour));
					et.add(Calendar.MINUTE, Integer.parseInt(end_min));
					et.add(Calendar.SECOND, Integer.parseInt(end_sec));

					ADType tmp = (ADType) factory.newInstance(Class
							.forName("org.dlese.adn.ADType"));
					tmp.setDate(begin);
					tmp.setTime(bt);
					tmp.setValue("");
					timeAD.setBegin(tmp);

					tmp = (ADType) factory.newInstance(Class
							.forName("org.dlese.adn.ADType"));
					tmp.setDate(end);
					tmp.setTime(et);
					tmp.setValue("");
					timeAD.setEnd(tmp);

				} else if (time_choice.equals("BC")) {

					TimeBCType timeBC = (TimeBCType) factory.newInstance(Class
							.forName("org.dlese.adn.TimeBCType"));
					timeInfo.setTimeBC(timeBC);

					timeBC.setBegin(bc_begin_year);
					timeBC.setEnd(bc_end_year);

				} else if (time_choice.equals("direct")) {

					TimeRelativeType timeRelative = (TimeRelativeType) factory
							.newInstance(Class
									.forName("org.dlese.adn.TimeRelativeType"));
					timeInfo.setTimeRelative(timeRelative);

					RelativeType begin = (RelativeType) factory
							.newInstance(Class
									.forName("org.dlese.adn.RelativeType"));
					timeRelative.setBegin(begin);
					begin.setValue(new BigDecimal("-" + d_begin_year));
					begin.setUnits("year");

					RelativeType end = (RelativeType) factory.newInstance(Class
							.forName("org.dlese.adn.RelativeType"));
					timeRelative.setEnd(end);
					end.setValue(new BigDecimal(d_end_year));
					end.setUnits("year");

				}
			}

			// handle object in space
			ObjectsInSpaceType objectsInSpace = (ObjectsInSpaceType) factory
					.newInstance(Class
							.forName("org.dlese.adn.ObjectsInSpaceType"));

			itemRecord.setGeneral(general);
			itemRecord.setLifecycle(lifecycle);
			itemRecord.setMetaMetadata(metaMetadata);
			itemRecord.setRights(rights);
			itemRecord.setTechnical(technical);
			// itemRecord.setRelations(relations);
			itemRecord.setGeospatialCoverages(geospatialCoverages);
			if (!time.equals("any")) {
				itemRecord.setTemporalCoverages(temporalCoverages);
			}
			itemRecord.setObjectsInSpace(objectsInSpace);

			// marshall
			JAXBContext jc = JAXBContext.newInstance("org.dlese.adn");
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			// create and save target

			// save into a directory
			File dir = new File("adn/" + uuid); // TODO: remove adn folder once
												// all the files have been
												// removed.
			dir.mkdirs();
			dirPath = dir.getAbsolutePath();

			// unzip the uploaded file
			String shpFile = shpURL;
			String unzipFileName = "";
			if (shpFile.trim().startsWith("http://")) {
				URL shpURL = new URL(shpFile);
				HttpURLConnection huc = (HttpURLConnection) shpURL
						.openConnection();
				huc.connect();
				InputStream in = huc.getInputStream();
				File zip = new File(dir, "tmp.zip");
				FileOutputStream out = new FileOutputStream(zip);
				byte[] buffer = new byte[1024];
				int count = in.read(buffer);
				while (count > 0) {
					out.write(buffer, 0, count);
					count = in.read(buffer);
				}
				huc.disconnect();
				out.close();
				unzipFileName = unzip(dir, zip); // Unzipping the ftpped file to
													// dir.
				zip.delete(); // the zip file is no longer necessary.
			} else if (shpFile.trim().startsWith("ftp://")) {
				shpFile = shpFile.substring(6);
				String username = "anonymous";
				String password = "geon@geongrid.org";
				int index = shpFile.indexOf("/");
				String hostname = shpFile.substring(0, index);
				String filename = shpFile.substring(index);
				org.apache.commons.net.ftp.FTPClient f = new org.apache.commons.net.ftp.FTPClient();
				f.connect(hostname);
				f.login(username, password);
				f.setFileType(FTP.BINARY_FILE_TYPE);
				File zip = new File(dir, "tmp.zip");
				FileOutputStream fos = new FileOutputStream(zip);
				f.retrieveFile(filename, fos);
				f.disconnect();
				fos.close();
				unzipFileName = unzip(dir, zip); // Unzipping the ftpped file to
													// dir.
				zip.delete(); // the zip file is no longer necessary.

			} else { // file is local..
				java.io.File zip = new java.io.File(shpFile);
				unzipFileName = unzip(dir, zip);
			}

			if (!unzipFileName.equals("")) {
				// calculate the binding box and set the adn schema
				shpfis = new FileInputStream(dirPath + "/" + unzipFileName
						+ ".shp");
				Shapefile shape = new Shapefile(shpfis);
				double[] bounds = shape.getBounds();

				box.setWestCoord(new BigDecimal(bounds[0]));
				box.setNorthCoord(new BigDecimal(bounds[1]));
				box.setEastCoord(new BigDecimal(bounds[2]));
				box.setSouthCoord(new BigDecimal(bounds[3]));

				shpfis.close();
				// Object x = (Object) shape;

				// shape = new Shapefile();

				/*
				 * File shp = new File(dir, unzipFileName + ".shp"); File shx =
				 * new File(dir, unzipFileName + ".shx"); File dbf = new
				 * File(dir, unzipFileName + ".dbf");
				 * 
				 * 
				 * shp.delete(); shx.delete(); dbf.delete(); //dir.delete();
				 */
			}
			/*
			 * // calculate the schema and ask for more explanation DBase db =
			 * new DBase(dirName); db.openTable(fileName); String [] columns =
			 * db.getColumnNames(); ArrayList list = new ArrayList(); for (int
			 * i=0; i<columns.length; i++) { list.add(columns[i]); }
			 */// save its metadata
			File adn = new File(dir, uuid + ".adn");
			FileOutputStream fos = new FileOutputStream(adn);
			m.marshal(itemRecord, fos);
			fos.close();

			/*
			 * } catch (Exception e) {
			 * 
			 * try { PrintWriter pw = new PrintWriter(new
			 * FileWriter("/home/jaeger/log.txt", true)); e.printStackTrace(pw);
			 * pw.flush(); } catch (Exception ex) {}
			 * 
			 * throw new JspException(e.getMessage()); } return SKIP_BODY; }
			 * 
			 * 
			 * 
			 * private String label(String string) { return
			 * "<table width=90% cellpadding=1 cellspacing=0 border=0>\n"+
			 * "<tr><td bgcolor=Gainsboro>\n"+
			 * "<font face=\"arial,sans-serif\" size=-1 color=#777777>\n"+
			 * "&nbsp; <b>"+string+"</b>\n"+ "</font>\n"+ "</td></tr>\n"+
			 * "</table>\n"; }
			 * 
			 * 
			 * private String message(String key, String val) { return
			 * "    <tr>\n" +
			 * "        <td align=right width=150><div class=label><b>"
			 * +key+":</b></div></td>\n" +
			 * "        <td align=left>"+val+"</td>\n" + "    </tr>\n"; }
			 * 
			 * 
			 * private String messagePadding(String key, String val) { return
			 * "    <tr>\n" +
			 * "        <td align=right width=150><div class=label>&nbsp;&nbsp;&nbsp;"
			 * +key+":</div></td>\n" + "        <td align=left>"+val+"</td>\n" +
			 * "    </tr>\n"; }
			 */
			return adn.getAbsolutePath();
		} catch (ClassNotFoundException cnfex) {
			cnfex.printStackTrace();
			_deleteFiles();
		} catch (JAXBException jex) {
			jex.printStackTrace();
			_deleteFiles();
		} catch (FileNotFoundException fnex) {
			fnex.printStackTrace();
			_deleteFiles();
		} catch (IOException ioex) {
			ioex.printStackTrace();
			_deleteFiles();
		} catch (ShapefileException shex) {
			shex.printStackTrace();
			_deleteFiles();
		} catch (Exception ex) {
			ex.printStackTrace();
			_deleteFiles();
		}
		return "";

	}

	public static String unzip(java.io.File dst, java.io.File zip)
			throws Exception {

		System.out.println("dst folder = " + dst.getAbsolutePath()
				+ " , zip file = " + zip.getAbsolutePath());
		int BUFFER = 2048;
		BufferedOutputStream dest = null;
		FileInputStream fis = new FileInputStream(zip);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
		String name = null;
		boolean error = false;
		while ((entry = zis.getNextEntry()) != null) {

			if (entry.isDirectory()) {
				continue;
			}

			String tmp = entry.getName();
			String nm = tmp;

			int index = tmp.lastIndexOf("/");
			if (index != -1) {
				tmp = tmp.substring(index + 1);
				nm = tmp;
			}

			if (tmp.endsWith("dbf") || tmp.endsWith("shp")
					|| tmp.endsWith("shx")) {

				tmp = tmp.substring(0, tmp.length() - 4);
				if (name == null) {
					name = tmp;
				} else if (name.equals(tmp)) {

				} else {
					continue;
				}
			}

			int count;
			byte data[] = new byte[BUFFER];

			// write the files to the disk
			java.io.File file = new java.io.File(dst, nm);
			FileOutputStream fos = new FileOutputStream(file);
			dest = new BufferedOutputStream(fos, BUFFER);
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
		}
		zis.close();
		fis.close();

		return name;

	}

	private void _deleteFiles() {
		shpfis = null;
		File dir = new File(dirPath);
		String fileNames[] = dir.list();
		for (int i = 0; i < fileNames.length; i++) {
			System.out.println(fileNames[i]);
			File file = new File(dir, fileNames[i]);
			file.delete();
		}
		File parent = dir.getParentFile();
		dir.delete();
		// if this is the adn dir and its empty - delete it.
		if (parent.getName().equals("adn") && parent.list().length == 0) {
			parent.delete();
		}
	}

	private FileInputStream shpfis = null;
	private String dirPath = "";
	private String shpURL = "";

}