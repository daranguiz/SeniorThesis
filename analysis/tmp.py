# -*- coding: utf-8 -*-

"""

Created on Thu Nov 05 11:13:47 2009



@author: Matti

"""



#blogiin

import scipy.signal as signal

from pylab import *



def mfreqz(b,a=1):

    w,h = signal.freqz(b,a)

    h_dB = 20 * log10 (abs(h))

    subplot(211)

    plot(w/max(w),h_dB)

    ylim(-150, 5)

    ylabel('Magnitude (db)')

    xlabel(r'Normalized Frequency (x$\pi$rad/sample)')

    title(r'Frequency response')

    subplot(212)

    h_Phase = unwrap(arctan2(imag(h),real(h)))

    plot(w/max(w),h_Phase)

    ylabel('Phase (radians)')

    xlabel(r'Normalized Frequency (x$\pi$rad/sample)')

    title(r'Phase response')

    subplots_adjust(hspace=0.5)



def impz(b,a=1):

        impulse = repeat(0.,50); impulse[0] =1.

        x = arange(0,50)

        response = signal.lfilter(b,a,impulse)

        subplot(211)

        stem(x, response)

        ylabel('Amplitude')

        xlabel(r'n (samples)')

        title(r'Impulse response')

        subplot(212)

        step = cumsum(response)

        stem(x, step)

        ylabel('Amplitude')

        xlabel(r'n (samples)')

        title(r'Step response')

        subplots_adjust(hspace=0.5)



#Bandpass IIR

b,a = signal.iirdesign(wp = [0.05, 0.3], ws= [0.02, 0.35], gstop= 60, gpass=1, ftype='ellip')

w, h = signal.freqz(b, a)
plot(w/(2*pi), 20*np.log10(abs(h)))


# mfreqz(b,a)

# figure(2)
#
# impz(b,a)

show()