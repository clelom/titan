/*
    This file is part of Titan.

    Titan is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Titan is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Titan. If not, see <http://www.gnu.org/licenses/>.
*/

/**
  * TitanConfigCache.nc
  *
  * Interface to the config cache. This cache stores configurations 
  * such that they can be started using a single message.
  *
  * Updates of the communication task are allowed.
  * TODO: specify how
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  */

interface TitanConfigCache {

  /**
  * Retrieves a full configuration from a TITANCOMM_CACHE_START message
  * @param pMsg a TITANCOMM_CACHE_START message specifying the configuration to load
  * @return the configuration entry, or NULL if not existing.
  */
  command TCCMsg* getCacheEntry( TCCMsg* pMsg );

  /**
  * Stores a TITANCOMM_CACHE_STORE to the cache for later startup
  * @param pMsg a message containing the partial configuration to be stored
  * @return whether successfull
  */
  command error_t storeCacheEntry( TCCMsg* pMsg );
  
  /**
  * Clears all configurations from the cache
  */
  command void clearAll();

}
