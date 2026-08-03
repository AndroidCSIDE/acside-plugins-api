/*
 *  This file is part of ACSIDE.
 *
 *  ACSIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ACSIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with ACSIDE.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.nullij.androidcodestudio.plugins.api.gate

/**
 * Marks IDE-internal APIs that must not be called from plugin code.
 *
 * Calling any symbol annotated with [@InternalPluginApi] without explicitly
 * opting in causes a compile-time error.
 *
 * @author nullij
 * @see https://github.com/nullij
 */
@RequiresOptIn(
    level   = RequiresOptIn.Level.ERROR,
    message = "This is an internal IDE API. Plugin code must never call it."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
annotation class InternalPluginApi