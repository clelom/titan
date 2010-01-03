%    This file is part of Titan.
%
%    Titan is free software: you can redistribute it and/or modify
%    it under the terms of the GNU General Public License as 
%    published by the Free Software Foundation, either version 3 of 
%    the License, or (at your option) any later version.
%
%    Titan is distributed in the hope that it will be useful,
%    but WITHOUT ANY WARRANTY; without even the implied warranty of
%    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
%    GNU General Public License for more details.
%
%    You should have received a copy of the GNU General Public License
%    along with Titan. If not, see <http://www.gnu.org/licenses/>.
%
%    Copyright 2009 Clemens Lombriser

% runs marker on the specified logfile
%
% Note: strfile should have the extension .txt
%
function run_marker(strfile,samplePeriod)

% %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% read file and organize structures
fprintf(1,'reading log file...\n');
in   = read_titan_log(strfile,samplePeriod);
fprintf(1,'parsing nodes...\n');
logs = sync_nodes(in,samplePeriod);


%% saving 
fprintf(1,'saving intermediate data...\n');
save(strrep(strfile,'.log','.mat'),'logs');

% load
%load 'datacom_pc.mat';

for logindex=1:length(logs)
    % determine length of the time
    times(logindex) = length(logs{logindex}.time);
end

labels = zeros(min(times(times>0)),1);

% %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% set up class information

i=1;

% wrist node
classes{i} = 'wrist: pick up';i=i+1;
classes{i} = 'wrist: put down';i=i+1;

% dice node
classes{i} = 'dice: pick up';i=i+1;
classes{i} = 'dice: shake';i=i+1;
classes{i} = 'dice: roll';i=i+1;

% combination
classes{i} = 'both: pick up';i=i+1;


% %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Check whether predictions exist
% %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% LOG file: datacom_XXX.log predictions: predictions_XXX.txt
strPredictions = strrep(strrep(strfile,'datacom','predictions'),'log','txt');

if exist(strPredictions,'file') == 2
    predictions = dlmread(strPredictions);
    predictions = predictions(:,2:3);
    
    pred_window = size(logs{1}.data,1) - size(predictions,1)+1;
    
    predictions = [ zeros(floor(pred_window/2),2); predictions; zeros(ceil(pred_window/2),2) ];
    
end


% %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Marker part
% %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
clear drawerobj;

fprintf(1,'Configuring marker...\n');

drawerobj.title = sprintf('Dice throwing experiment (%s)',strfile);
drawerobj.labelstrings = classes;
drawerobj.maxLabelNum  = length(drawerobj.labelstrings);

% set options
initlabels = labeling2segments(labels,0);
%for i=1:length(logs)
%    PlotTitle{i} = sprintf('Node %i',i);
%end
PlotTitle = {'Wrist','Dice'};

% configure each Marker plot
for sysno = 1:length(logs)
    %drawerobj.disp(sysno).type = 'WAV'; % data type, e.g. WAV, XSENS...
    
    drawerobj.disp(sysno).data = logs{sysno}.data(1:min(times(times>0)),:); % [samples, channels] = size(data)
    
	% plot routine used to display data, default: plot()
	% options
	% @plot: Use this function for all time series signals
	% @marker_plotlabel: Use this function to plot labels in a subplot, parameters: maxclasses.
	%drawerobj.disp(sysno).plotfunc = @plot;
	
	% Cell array of additional parameters for plot routine, optional
	% Default 'drawerobj.disp(sysno).plotfunc_params = {'LineWidth', 2}' is
	% used when field is empty. To omit this, e.g. when using another plot
	% function, use drawerobj.disp(sysno).plotfunc_params = {' '};
	drawerobj.disp(sysno).plotfunc_params = {'LineWidth', 1};
	
    
	% y-axis title, optional
	drawerobj.disp(sysno).ylabel = [PlotTitle{sysno} ' [g]'];
    
    % reference sampling rate [Hz] for the data, must be equal for all plots
    drawerobj.disp(sysno).sfreq = 1/samplePeriod/10^(-3);
    
    % y-axis resolution, optinally (default: guessed automatically)
    drawerobj.disp(sysno).ylim = [0 4096];
    
    % alignment shift in samples, optional
    %drawerobj.disp(sysno).alignshift = 0;
    
    % alignment sample rate (relative to sfreq), optional
    %drawerobj.disp(sysno).alignsps = 0;

    % size of the data, optional
	%drawerobj.disp(sysno).datasize = size(drawerobj.disp(sysno).data,1);

    % initial visible data range (x-axis), optional
%    drawerobj.disp(sysno).xvisible = drawerobj.disp(sysno).sfreq*300;
    drawerobj.disp(sysno).xvisible = length(drawerobj.disp(sysno).data);

    % set signal names
    drawerobj.disp(sysno).signalnames = {'Acceleration X', 'Acceleration Y', 'Acceleration Z' };
	
	% config label visibility
	if (sysno==1)
		% Labels may be shown/hidden for each plot to improve visibility.
		% Setting an label map index to zero hides the corresponding label
		% in the plot. Default: all enabled.
		drawerobj.disp(sysno).showlabels = [ 1 1 0 0 0 0 ];
        
    elseif (sysno==2)
		drawerobj.disp(sysno).showlabels = [ 0 0 1 1 1 0 ];
	end;

	
	% config player information
    % you may listen to the sound section or display a aviatar
    if (0)
        % here we provide some optional infos to play sound
        % you may register your "play" method in marker_player()
		% several player sources can be configured for each plot by
		% extending the array drawerobj.disp(sysno).playerdata(). Source is
		% selectable through drawerobj.disp(splot).playersource and Shift+p 
        %drawerobj.disp(sysno).playerdata.sourcefile = 'data/mywavfile.wav'; % needed for WAV
        %drawerobj.disp(sysno).playerdata.playchannel = 1; % optional, WAV only
		%sdrawerobj.disp(sysno).playerdata.gain = 1; % optional, WAV only
    end;
    
	
    % on-demand data load
	%
    % When using this mode, set drawerobj.disp(sysno).data = []
    % The following example is valid for WAV audio data. Other data types
    % require a specific load function.
    % 
    % Determine size and sample rate of a WAV file:
    % [dummy, WAVSize, WAVRate] = WAVReader(WAVFile);
    %
    %drawerobj.disp(sysno).data = [];
    %drawerobj.disp(sysno).loadfunc = @WAVReader;
    %drawerobj.disp(sysno).loadfunc_params = {'option1', 1}; % <= optional
    %drawerobj.disp(sysno).loadfunc_filename = WAVFile;
    %
    % All other fields may be configured as usual.
end;

% check if predictions exist
if exist('predictions','var') ~= 0
    sysno = sysno+1;
    drawerobj.disp(sysno).data = predictions;
%    drawerobj.disp(sysno).plotfunc_params = {'LineWidth', 1};
%    drawerobj.disp(sysno).ylabel = [PlotTitle{sysno} ' [g]'];
    drawerobj.disp(sysno).sfreq = 1/samplePeriod/10^(-3);
%    drawerobj.disp(sysno).ylim = [-1 5];
%    %drawerobj.disp(sysno).alignshift = 0;
%    %drawerobj.disp(sysno).alignsps = 0;
%	%drawerobj.disp(sysno).datasize = size(drawerobj.disp(sysno).data,1);
%    %drawerobj.disp(sysno).xvisible = length(drawerobj.disp(sysno).data);
    drawerobj.disp(sysno).signalnames = {'Label', 'prediction' };
%    drawerobj.disp(sysno).showlabels = [ zeros(1,length(classes)) ];
end

% configure a default file name (suggested when saving a label file), optional
[fdir fname fext] = fileparts('mylabelfile.mat');
drawerobj.iofilename = [fname fext];
drawerobj.defaultDir = fdir;

% Marker window size: [height width], optional
% drawerobj.windowsizescaling = [0.7 1];


% Launch Marker by supplying drawerobj struct and initlabels.
% Alternatively labels can be stored in drawerobj.seglist, while initlabels = [].
fprintf('\n%s: Launching Marker...', mfilename);
marker(drawerobj, initlabels);


% clear up
clear initlabels;
