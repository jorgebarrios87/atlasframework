/*
 ** Rachel  - Resource Loading Toolkit for Web Start/JNLP
 ** Copyright (c) 2001, 2002 by Gerald Bauer
 **
 ** This program is free software.
 **
 ** You may redistribute it and/or modify it under the terms of the GNU
 ** General Public License as published by the Free Software Foundation.
 ** Version 2 of the license should be included with this distribution in
 ** the file LICENSE, as well as License.html. If the license is not
 ** included with this distribution, you may find a copy at the FSF web
 ** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
 ** Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
 **
 ** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
 ** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
 ** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
 ** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
 ** REDISTRIBUTION OF THIS SOFTWARE.
 **
 */

package rachel.url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class RachelUrlFactory implements URLStreamHandlerFactory {
	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (protocol.equalsIgnoreCase("class")) {
			return new ClassUrlStreamHandler();
		} else
			return null;
	}
}
