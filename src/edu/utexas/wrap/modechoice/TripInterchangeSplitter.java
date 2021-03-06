/*
    wrap - free and open-source urban transportation modeling software
    Copyright (C) 2017 the wrap project, The University of Texas at Austin

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.utexas.wrap.modechoice;

import java.util.stream.Stream;

import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.ModalPAMatrix;

/**The default method of performing mode choice,
 * this method calculates mode shares as a function
 * of the distributed trip matrix (as opposed to just
 * raw, unlinked productions and attractions). This 
 * model assumes that trips are not bound to a given
 * mode, i.e. they are not captive riders.
 * 
 * @author William
 *
 */
public interface TripInterchangeSplitter {
	
	public Stream<ModalPAMatrix> split(AggregatePAMatrix aggregate);
	
}
